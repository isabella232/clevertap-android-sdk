package com.clevertap.android.xps

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.clevertap.android.xps.XpsConstants.FAILED_WITH_EXCEPTION
import com.clevertap.android.xps.XpsConstants.OTHER_COMMAND
import com.clevertap.android.xps.XpsConstants.TOKEN_SUCCESS
import com.clevertap.android.xps.XpsTestConstants.Companion.MI_TOKEN
import com.xiaomi.mipush.sdk.ErrorCode.ERROR_SERVICE_UNAVAILABLE
import com.xiaomi.mipush.sdk.ErrorCode.SUCCESS
import com.xiaomi.mipush.sdk.MiPushClient.COMMAND_REGISTER
import com.xiaomi.mipush.sdk.MiPushClient.COMMAND_UNSET_ACCOUNT
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiMessageHandlerTest : BaseTestCase() {

    private lateinit var handler: XiaomiMessageHandlerImpl
    private lateinit var parser: IXiaomiNotificationParser

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        parser = mock(XiaomiNotificationParser::class.java)
        handler = XiaomiMessageHandlerImpl(parser)
    }

    @Test
    fun testCreateNotification_Null_Message() {
        val isSuccess = handler.createNotification(application, null)
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Invalid_Message_Throws_Exception() {
        val isSuccess = handler.createNotification(application, MiPushMessage())
        val bundle = Bundle()
        `when`(parser.toBundle(any(MiPushMessage::class.java))).thenReturn(bundle)
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.createNotification(application, bundle)).thenThrow(
                RuntimeException("Something went wrong")
            )
            Assert.assertFalse(isSuccess)
        }
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Valid_Message() {
        `when`(parser.toBundle(any(MiPushMessage::class.java))).thenReturn(Bundle())
        val isSuccess = handler.createNotification(application, MiPushMessage())
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testOnReceivePassThroughMessage_Other_Command() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_UNSET_ACCOUNT)
        val result = handler.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, OTHER_COMMAND)
    }

    @Test
    fun testOnReceivePassThroughMessage_Token_Success() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenReturn(listOf(MI_TOKEN))
        val result = handler.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, TOKEN_SUCCESS)
    }

    @Test
    fun testOnReceivePassThroughMessage_Invalid_Token() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenReturn(emptyList())
        val result = handler.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, XpsConstants.INVALID_TOKEN)
    }

    @Test
    fun testOnReceivePassThroughMessage_Token_Failure() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(ERROR_SERVICE_UNAVAILABLE.toLong())
        `when`(message.commandArguments).thenReturn(emptyList())
        val result = handler.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, XpsConstants.TOKEN_FAILURE)
    }

    @Test
    fun testOnReceivePassThroughMessage_Failed_Exception() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenThrow(RuntimeException("Something went wrong"))
        val result = handler.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, FAILED_WITH_EXCEPTION)
    }
}