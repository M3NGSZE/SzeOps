pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                script{
                    // you need to repleace the value here {token} at def
                    def token="token"
                    def chatId="token"
                    def message1="""
                    hello world
                    welcome to jenkins telelgram message 2
                    """
                    // sendTelegramMessage("${message1}", "${token}", "${chatId}")

                    def message2="""
                    *welcome to jenkins telelgram message*
                    using second message
                    """
                    sendTelegramMessageV1("${message2}", "${token}", "${chatId}")
                }
            }
        }
    }
}

def sendTelegramMessageV1(String message, String token, String chatId) {
    sh """ 
        curl -X POST https://api.telegram.org/bot${token}/sendMessage \
        -d chat_id="${chatId}" -d parse_mode="Markdown" -d text="${message}" 
    
    """
}

def sendTelegramMessage(String message, String token, String chatId) {
    // upgrade to use Markdown versin instead
    def encodedMessage = URLEncoder.encode(message, "UTF-8")
    sh """ 
        curl -X POST https://api.telegram.org/bot${token}/sendMessage \\
        -d chat_id="${chatId}" \\
        -d text="${encodedMessage}" > /dev/null
    
    """
}
