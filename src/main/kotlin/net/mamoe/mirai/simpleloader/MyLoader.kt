package net.mamoe.mirai.simpleloader

import io.ktor.client.*
import io.ktor.client.request.*
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
import net.mamoe.mirai.message.sendImage
import redis.clients.jedis.Jedis
import simplelolicon.fetchRemote
import java.io.File
import java.io.InputStream
import kotlin.random.Random

// 实际上是隔壁的包
fun getKey(id: Long): String {
    return "h:$id:count"
}

fun getRussiaGameKey(id: Long): String {
    return "getRussiaGameKey:$id"
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
        "#开枪" {
            var remaining = jedis.get(getRussiaGameKey(group.id)).toInt()

            if (remaining == 0) {
                reply("#装弹子弹 <子弹数量=默认群成员数> 开始游戏 \n例如: #装填子弹 10")
            } else {
                if (Random.nextInt(0, remaining) == 0) {
                    remaining = 0
                    sender.mute(60 * 10)
                    reply(At(sender) + "您被击中了, 游戏结束")
                } else {
                    remaining -= 1
                    reply(At(sender) + "您的运气好, 没有被击中。 剩余子弹:${remaining}")
                }
                jedis.set(getRussiaGameKey(group.id), remaining.toString())
            }
        }

        "#roll" {
            val members = group.members
            val rollMember = members.elementAt(Random.nextInt(members.size))
            reply(At(rollMember) + " 整挺好😅 ")
        }

        startsWith("#h", removePrefix = true) {
            val lolicon = fetchRemote(it)
            lolicon.forEach {
                val loliconstream = HttpClient().use { client -> client.get<InputStream>(it.url) }
//                val path = "lolicon/caches"
//                File(path).mkdir()
//                val filename = "$path/${it.title}.jpg"
//                File(filename).writeBytes(response)
                group.sendImage(loliconstream)
            }
        }

        startsWith("#装填子弹", removePrefix = true) {
            val count = it.toIntOrNull() ?: group.members.size
            jedis.set(getRussiaGameKey(group.id), count.toString())
            reply("装填子弹成功, 数量$count。 游戏开始, 请输入 #开枪 参与游戏")
        }

        // wtf?
        has<At> {
            if (message[At]?.target == bot.id) {
                val text = message.firstOrNull(PlainText)
                var answer = "差不多得了😅"
                if (text != null) {
                    // reply from redis KV
                    try {
                        answer = jedis.get(text.contentToString().trim())
                    } finally {
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
        if (message.content.startsWith("#")) {
            // wait
        } else {
            messageArrayList.add(message)

            val sameMessageListLength = (messageArrayList.filter {
                it.content == message.content
            }).size

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
}
