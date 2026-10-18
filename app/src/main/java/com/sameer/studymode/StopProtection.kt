
package com.sameer.studymode

data class ArithmeticQuestion(
    val text: String,
    val answer: Int
)

object StopProtection {

    fun generateQuestion(): ArithmeticQuestion {
        val a = (12..29).random()
        val b = (2..6).random()
        val c = (10..49).random()

        val product = a * b
        val partial = product + c
        val d = (1..partial).random()

        val answer = partial - d

        return ArithmeticQuestion(
            text = "$a × $b + $c - $d = ?",
            answer = answer
        )
    }

    fun isCorrect(
        userAnswer: String,
        correctAnswer: Int
    ): Boolean {
        return userAnswer.trim().toIntOrNull() == correctAnswer
    }
}
