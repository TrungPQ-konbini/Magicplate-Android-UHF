package com.konbini.magicplateuhf.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.io.UnsupportedEncodingException

class MqttHelper(
    context: Context,
    serverUri: String,
    subscriptionTopic: String,
    username: String,
    password: String
) {
     lateinit var mqttAndroidClient: MqttAndroidClient

    var serverUri: String = serverUri
    val clientId: String = MqttClient.generateClientId()
    var subscriptionTopic: String = subscriptionTopic

    var username: String = username
    var password: String = password

    var context: Context = context

    fun init() {
        mqttAndroidClient = MqttAndroidClient(context, this.serverUri, clientId)
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                Log.w("mqtt", s)
            }

            override fun connectionLost(throwable: Throwable) {}

            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                Log.w("Mqtt", mqttMessage.toString())
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {}
        })
        connect()
    }

    fun setCallback(callback: MqttCallbackExtended?) {
        mqttAndroidClient.setCallback(callback)
    }

    private fun connect() {
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.userName = username
        mqttConnectOptions.password = password.toCharArray()
        mqttConnectOptions.connectionTimeout = 3
        mqttConnectOptions.keepAliveInterval = 60
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions = DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    subscribeToTopic()
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.w("Mqtt", "Failed to connect to: $serverUri$exception")
                    Toast.makeText(context, "MQTT Connect Fails", Toast.LENGTH_LONG).show()
                    LogUtils.logInfo("MQTT Connect Fails")
                }
            })
        } catch (ex: MqttException) {
            LogUtils.logError(ex)
        }
    }

    private fun subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.w("Mqtt", "Subscribed!")
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    Log.w("Mqtt", "Subscribed fail!")
                }
            })
        } catch (ex: MqttException) {
            //System.err.println("Exception whilst subscribing")
            LogUtils.logInfo("Exception whilst subscribing")
            LogUtils.logError(ex)
        }
    }

    fun publishToTopic(payload: String) {
        try {
            var encodedPayload = ByteArray(0)
            encodedPayload = payload.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            mqttAndroidClient.publish(subscriptionTopic, message)
            Log.w("Mqtt", "Message published")
        } catch (ex: MqttException) {
            //System.err.println("Error Publishing")
            LogUtils.logInfo("Error Publishing")
            LogUtils.logError(ex)
        } catch (ex: UnsupportedEncodingException) {
            //System.err.println("Error Publishing")
            LogUtils.logInfo("Error Publishing")
            LogUtils.logError(ex)
        }
    }

    fun isConnected(): Boolean {
        return mqttAndroidClient.isConnected
    }

    @Throws(MqttException::class)
    private fun destroy() {
        mqttAndroidClient.unregisterResources()
        mqttAndroidClient.disconnect()
    }
}