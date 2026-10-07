package com.snurdy.backend.domain.inquiry.mail.sender

import com.snurdy.backend.domain.inquiry.config.InquiryProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.ses.SesClient
import java.util.Base64

@Component
class InquiryMailSender(
    private val sesClient: SesClient,
    private val inquiryProperties: InquiryProperties
) {
    private val source: String = inquiryProperties.mail.let { encodeSource(it.senderName, it.sender) }

    fun sendMail(content: InquiryMailContent) {
        sesClient.sendEmail { request ->
            request
                .source(source)
                .destination { it.toAddresses(inquiryProperties.mail.receiver) }
                .message { message ->
                    message
                        .subject { it.data(content.subject).charset(CHARSET) }
                        .body { body -> body.text { it.data(content.body).charset(CHARSET) } }
                }
        }
    }

    private fun encodeSource(senderName: String, senderAddress: String): String {
        val encodedName = Base64.getEncoder().encodeToString(senderName.toByteArray(Charsets.UTF_8))
        return "=?UTF-8?B?$encodedName?= <$senderAddress>"
    }

    companion object {
        private const val CHARSET = "UTF-8"
    }
}