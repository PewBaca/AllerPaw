package com.allerpaw.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.allerpaw.app.data.local.entity.TaskEntity
import com.allerpaw.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

const val NOTIFICATION_CHANNEL_ID   = "allerpaw_tasks"
const val NOTIFICATION_CHANNEL_NAME = "AllerPaw Aufgaben"

/**
 * WorkManager Worker für Intervall-basierte Task-Benachrichtigungen.
 * Unterstützt: alle N Tage (Intervall), täglich, wöchentlich, einmalig.
 * Läuft täglich um Mitternacht und prüft welche Tasks heute fällig sind.
 */
@HiltWorker
class TaskNotificationWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val taskRepo: TaskRepository
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val heute = LocalDate.now()
        taskRepo.getAllWithPush().forEach { task ->
            if (isTaskFaelligHeute(task, heute)) {
                val bereitsErledigt = taskRepo.getErledigung(task.id, heute)?.erledigt == true
                if (!bereitsErledigt) sendNotification(applicationContext, task)
            }
        }
        return Result.success()
    }

    private fun isTaskFaelligHeute(task: TaskEntity, heute: LocalDate): Boolean =
        when (task.wiederholung) {
            "taeglich"     -> true
            "einmalig"     -> false
            "woechentlich" -> {
                val tage = task.wochentage.split(",").mapNotNull { it.trim().toIntOrNull() }
                heute.dayOfWeek.value in tage
            }
            "intervall" -> {
                // Prüfe ob heute ein Intervall-Tag ist:
                // Startpunkt = createdAt-Datum, Abstand = intervallTage
                val startEpoch = task.createdAt.toEpochMilli()
                val startDate  = java.time.Instant.ofEpochMilli(startEpoch)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                val diff = java.time.temporal.ChronoUnit.DAYS.between(startDate, heute)
                diff >= 0 && diff % task.intervallTage == 0L
            }
            else -> false
        }

    companion object {
        fun createChannel(ctx: Context) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Erinnerungen für wiederkehrende Aufgaben" }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun sendNotification(ctx: Context, task: TaskEntity) {
            val emoji = when (task.kategorie) {
                "medikament" -> "💊"
                "pflege"     -> "🛁"
                "tierarzt"   -> "🏥"
                else         -> "📋"
            }
            val notification = androidx.core.app.NotificationCompat
                .Builder(ctx, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle("$emoji ${task.titel}")
                .setContentText(task.beschreibung.ifBlank { "Aufgabe für heute bestätigen" })
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            ctx.getSystemService(NotificationManager::class.java)
                .notify(task.id.toInt(), notification)
        }

        /** Plant den täglichen Check auf Mitternacht. */
        fun schedule(ctx: Context) {
            val jetzt       = LocalDateTime.now()
            val mitternacht = jetzt.toLocalDate().plusDays(1).atStartOfDay()
            val delayMin    = Duration.between(jetzt, mitternacht).toMinutes()

            val request = PeriodicWorkRequestBuilder<TaskNotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .addTag("task_notifications")
                .build()

            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "task_daily_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(ctx: Context) {
            WorkManager.getInstance(ctx).cancelAllWorkByTag("task_notifications")
        }
    }
}
