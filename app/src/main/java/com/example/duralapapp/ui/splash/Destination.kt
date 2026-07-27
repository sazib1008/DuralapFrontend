package com.example.duralapapp.ui.splash

sealed class Destination(val route: String) {
    data object Splash : Destination("splash")
    data object Login : Destination("login")
    data object Register : Destination("register")
    data object Home : Destination("home")
    data object Profile : Destination("profile")
    data object Search : Destination("search")
    data object Requests : Destination("requests")
    data object ChatDetail : Destination("chat_detail/{conversationId}/{recipientName}") {
        fun createRoute(conversationId: String, recipientName: String) = "chat_detail/$conversationId/$recipientName"
    }
    data object Call : Destination("call/{targetUserId}/{targetUserName}/{conversationId}/{callType}") {
        fun createRoute(targetUserId: String, targetUserName: String, conversationId: String, callType: String) =
            "call/$targetUserId/$targetUserName/$conversationId/$callType"
    }
}
