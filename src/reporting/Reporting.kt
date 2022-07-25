package org.synthesis.reporting

data class FormatOne(
    val requester_country: String?,
    val rows: Int?,
    val male: Int?,
    val female: Int?,
    val other: Int?
)

data class FormatTwo(
    val requester: String,
    val email: String,
    val projectTitle: String,
    val institution: String,
    val daysOfVisit: String
)

data class FormatThree(
    val requester: String,
    val email: String,
    val gender: String
)

data class FormatFour(
    val requester: String,
    val role: String,
    val gender: String
)
