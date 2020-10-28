package net.mamoe.mirai.simpleloader

import kotlinx.coroutines.delay
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeFriendMessages
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.join
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.nextMessage
import redis.clients.jedis.Jedis

// 实际上是隔壁的包
fun getKey(id: Long): String {
    return "h:$id:count"
}

val jedis = Jedis("localhost")
val messageArrayList = ArrayList<MessageChain>()


suspend fun main() {
    val bot = Bot(2274574101L, "deleterious666") {
        fileBasedDeviceInfo("device.json")
    }.alsoLogin()//新建Bot并登录


    bot.messageDSL()
    directlySubscribe(bot)

    bot.join() // 等待 Bot 离线, 避免主线程退出
}

fun Bot.messageDSL() {
    this.subscribeGroupMessages {
        // wtf?
        has<At> {
            if (message[At]?.target == bot.id) {
                val text = message.firstOrNull(PlainText)
                var answer = "差不多得了😅"
                if (text != null) {
                    try {
                        answer = jedis.get(text.contentToString().trim())
                    } catch (e: Exception) {
                        reply(answer)
                    }
                } else {
                    reply(answer)
                }
            }
        }

        "6" {
            reply("还行8")
        }

        "查询信用点" {
            val roll = (0..1000).random()
            reply("你的信用点剩余: $roll")
        }

        "roll个群友" {
            val members = this.group.members
            val membersLength = members.size - 1
            val rollIndex = (0..membersLength).random()
            val rollMember = members.elementAt(rollIndex)
            val atRoll = At(rollMember)
            reply(atRoll + " 整挺好😅 ")
        }

    }

    this.subscribeFriendMessages {
        startsWith("Q=", removePrefix = true) {
            val value = nextMessage<FriendMessageEvent> { true }
            jedis.set(it, value.content)
            reply("Got!")
        }
    }
}

/**
 * 监听单个事件
 */
suspend fun directlySubscribe(bot: Bot) {
    bot.subscribeAlways<GroupMessageEvent> {
        messageArrayList.add(message)

        val sameMessageListLength = (messageArrayList.filter {
            it.content == message.content
        }).size


        print(sameMessageListLength)
        if (sameMessageListLength >= 3) {
            messageArrayList.clear()
            delay(((1..2).random() * 1000).toLong())
            reply(message)
        }
        if (messageArrayList.size > 8) {
            messageArrayList.removeAt(0)
        }
    }
}