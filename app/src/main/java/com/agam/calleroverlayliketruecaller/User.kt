package com.agam.calleroverlayliketruecaller

data class User(
    val name: String,
    val number: String,
    val movieName: String,
    val place: String
)

fun findUserByNumber(number: String): User? {
    val fakeUserData = listOf(
        User(
            name = "Keanu Reeves",
            number = "1111111111",
            movieName = "John Wick (Chapter 4)",
            place = "Beirut, Lebanon" // His birthplace
        ),
        User(
            name = "Chris Evans",
            number = "2222222222",
            movieName = "Captain America: The First Avenger",
            place = "Boston, MA"
        ),
        User(
            name = "Scarlett Johansson",
            number = "3333333333",
            movieName = "Black Widow",
            place = "New York, NY"
        ),
        User(
            name = "Robert Downey Jr.",
            number = "4444444444",
            movieName = "Iron Man",
            place = "Los Angeles, CA"
        ),
        User(
            name = "Gal Gadot",
            number = "5555555555",
            movieName = "Wonder Woman",
            place = "Rosh HaAyin, Israel"
        )
    )

    return fakeUserData.find { user ->
        user.number == number
    }
}