package org.kotlinacademy.backend.usecases

import org.kotlinacademy.backend.Config
import org.kotlinacademy.backend.repositories.db.DatabaseRepository
import org.kotlinacademy.backend.repositories.email.EmailRepository
import org.kotlinacademy.data.Feedback

suspend fun sendEmailWithInfoAboutFeedback(feedback: Feedback, emailRepository: EmailRepository, databaseRepository: DatabaseRepository) {
    val adminEmail = Config.adminEmail ?: return
    val feedbackTo = feedback.newsId?.let { databaseRepository.getNews(it) }?.title ?: "Kotlin Academy"
    emailRepository.sendEmail(
            to = adminEmail,
            title = "New feedback",
            message = """
                New feedback to $feedbackTo
                Rating: ${feedback.rating}
                Comment:
                ${feedback.comment}

                Suggestions:
                ${feedback.suggestions}
            """.trimIndent()
    )
}