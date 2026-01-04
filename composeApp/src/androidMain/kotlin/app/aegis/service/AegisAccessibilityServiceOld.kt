package app.aegis.service

//
//class AegisAccessibilityServiceOld : AccessibilityService() {
//
//    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
//    private val geminiClient = GeminiClient()
//    private lateinit var overlayManager: OverlayManager
//
//    private var lastAnalysisTime = 0L
//    private val DEBOUNCE_TIME = 2000L // Analyze max once every 2 seconds
//
//    override fun onServiceConnected() {
//        super.onServiceConnected()
//        overlayManager = OverlayManager(this)
//        println("Aegis: Service Connected")
//    }
//
//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        if (event == null) return
//
//        // 1. Filter: Only care about text changes in target apps
//        if (event.packageName == "com.whatsapp" || event.packageName == "org.telegram.messenger") {
//
//            val currentTime = System.currentTimeMillis()
//            if (currentTime - lastAnalysisTime < DEBOUNCE_TIME) return
//
//            // 2. Extract Text (DFS Traversal)
//            val rootNode = rootInActiveWindow ?: return
//            val screenText = extractText(rootNode)
//
//            // 3. Analyze in Background
//            if (screenText.isNotEmpty()) {
//                lastAnalysisTime = currentTime
//
//                serviceScope.launch {
//                    println("Aegis: Analyzing -> ${screenText.take(50)}...")
//                    val verdict = geminiClient.analyze(screenText)
//
//                    if (verdict.riskLevel == RiskLevel.DANGER) {
//                        println("Aegis: DANGER DETECTED!")
//                        overlayManager.showShield(verdict.reason) {
//                            // On Unlock callback
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun extractText(node: AccessibilityNodeInfo?): String {
//        if (node == null) return ""
//        val sb = StringBuilder()
//
//        if (node.text != null && node.text.isNotEmpty()) {
//            sb.append(node.text).append("\n")
//        }
//
//        for (i in 0 until node.childCount) {
//            sb.append(extractText(node.getChild(i)))
//        }
//        return sb.toString()
//    }
//
//    override fun onInterrupt() {
//        serviceScope.cancel()
//    }
//}

