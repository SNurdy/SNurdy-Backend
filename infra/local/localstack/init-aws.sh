#!/bin/bash
set -euo pipefail

# 로컬 재전달 테스트를 빠르게 하려고 visibility timeout 을 운영 60초 대신 10초로 둔다
DLQ_URL=$(awslocal sqs create-queue \
  --queue-name inquiry-mail-dlq \
  --attributes MessageRetentionPeriod=1209600 \
  --query QueueUrl --output text)

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$DLQ_URL" \
  --attribute-names QueueArn \
  --query Attributes.QueueArn --output text)

cat > /tmp/inquiry-mail-attributes.json <<EOF
{
  "VisibilityTimeout": "10",
  "ReceiveMessageWaitTimeSeconds": "20",
  "RedrivePolicy": "{\"deadLetterTargetArn\":\"${DLQ_ARN}\",\"maxReceiveCount\":\"5\"}"
}
EOF

awslocal sqs create-queue \
  --queue-name inquiry-mail \
  --attributes file:///tmp/inquiry-mail-attributes.json

# LocalStack SES 도 발신 주소 인증을 요구한다
awslocal ses verify-email-identity --email-address inquiry@snurdy.org

echo "snurdy local aws resources ready"