package com.senseauto.nui_service

import com.senseauto.nui_service.dialogue.DialogueManager
import com.senseauto.nui_service.entity.ChatData
import com.senseauto.nui_service.entity.PositionType
import com.senseauto.nui_service.entity.Role
import com.senseauto.nui_service.nlu.SimpleNLU
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    DialogueManager.init(SimpleNLU)


    runBlocking {
        launch {
            val chat = ChatData(
                role = Role.User,
                content = "今天天气怎么样",
                userId = "1234",
                position = PositionType.Driver
            )
            DialogueManager.handleInput(chat)
        }

        delay(9000)
        launch {
            val chat = ChatData(
                role = Role.User,
                content = "讲个故事",
                userId = "12345",
                position = PositionType.Copilot
            )
            DialogueManager.handleInput(chat)
        }

        delay(3000)
        DialogueManager.snapshotRecord("1234")

        awaitCancellation()
    }
//        delay(5000)
//        print("\n")
//
//        launch {
//            val chat = ChatData(
//                role = Role.User,
//                content = "无意义的字段",
//                userId = "1234",
//            )
//            DialogueManager.handleInput(chat)
//        }
//
//
//        delay(3000)
//        print("\n")
//        launch {
//            val chat = ChatData(
//                role = Role.User,
//                content = "讲个故事",
//                userId = "12345",
//                position = PositionType.Copilot
//            )
//            DialogueManager.handleInput(chat)
//        }

//        delay(1000)
//        print("\n")
//        launch {
//            val chat = ChatData(
//                role = Role.User,
//                content = "无意义的字段",
//                userId = "12345",
//                position = PositionType.Copilot
//            )
//            DialogueManager.handleInput(chat)
//        }



//        delay(5000)
//        DialogueManager.snapshotRecord("1234")
//        DialogueManager.snapshotRecord("12345")

//        awaitCancellation()
//    }
}
