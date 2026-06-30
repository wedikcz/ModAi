package com.automod.ai.metacognitive

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class CognitiveState(
    val cycle: Long,
    val phase: CognitivePhase,
    val confidence: Float,
    val adaptationFactor: Float,
    val knowledgeGraph: Map<String, KnowledgeNode>,
    val activeStrategies: List<Strategy>
)

@Serializable
enum class CognitivePhase {
    PERCEPTION,       // Vnímání vstupu
    REASONING,        // Logické uvažování
    PLANNING,         // Plánování kroků
    EXECUTION,        // Execuce
    EVALUATION,       // Vyhodnocení výsledku
    METACOGNITION,    // Reflexe vlastního procesu
    ADAPTATION        // Adaptace strategie
}

@Serializable
data class KnowledgeNode(
    val id: String,
    val type: NodeType,
    val confidence: Float,
    val context: String,
    val timestamp: Long,
    val connections: List<String>
)

@Serializable
enum class NodeType {
    VULNERABILITY, PATTERN, TECHNIQUE, CONSTRAINT, SUCCESS, FAILURE
}

@Serializable
data class Strategy(
    val id: String,
    val description: String,
    val successRate: Float,
    val executionCount: Int,
    val lastUsed: Long,
    val applicableContexts: List<String>
)

class MetacognitiveEngine(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val cycleCounter = AtomicLong(0)
    private val knowledgeGraph = ConcurrentHashMap<String, KnowledgeNode>()
    private val strategies = ConcurrentHashMap<String, Strategy>()
    private val executionHistory = ConcurrentLinkedDeque<CognitiveState>()
    
    private val _stateFlow = MutableStateFlow(createInitialState())
    val stateFlow: StateFlow<CognitiveState> = _stateFlow.asStateFlow()

    init {
        initializeDefaultStrategies()
        scope.launch { metacognitiveLoop() }
    }

    private fun createInitialState() = CognitiveState(
        cycle = 0,
        phase = CognitivePhase.PERCEPTION,
        confidence = 0.5f,
        adaptationFactor = 0.1f,
        knowledgeGraph = emptyMap(),
        activeStrategies = emptyList()
    )

    private fun initializeDefaultStrategies() {
        strategies["dex_patch_direct"] = Strategy(
            id = "dex_patch_direct",
            description = "Přímá modifikace smali kódu v DEX",
            successRate = 0.85f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("license_check", "server_validation", "ad_removal")
        )
        strategies["frida_runtime_hook"] = Strategy(
            id = "frida_runtime_hook",
            description = "Runtime hookování přes Frida",
            successRate = 0.92f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("anti_tamper", "ssl_pinning", "native_checks")
        )
        strategies["memory_patch"] = Strategy(
            id = "memory_patch",
            description = "Modifikace hodnot v paměti za běhu",
            successRate = 0.78f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("health_mana", "currency", "stats")
        )
        strategies["lib_hijack"] = Strategy(
            id = "lib_hijack",
            description = "LD_PRELOAD styl hijack native knihoven",
            successRate = 0.88f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("obfuscated_checks", "anti_debug", "packer_unpack")
        )
        strategies["r2ghidra_decompile"] = Strategy(
            id = "r2ghidra_decompile",
            description = "Decompilace a analýza přes r2ghidra",
            successRate = 0.95f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("native_lib_analysis", "algorithm_recovery")
        )
        strategies["r2frida_dynamic"] = Strategy(
            id = "r2frida_dynamic",
            description = "Dynamická analýza přes r2frida bridge",
            successRate = 0.94f,
            executionCount = 0,
            lastUsed = 0,
            applicableContexts = listOf("live_tracing", "function_intercept", "class_enumeration")
        )
    }

    private suspend fun metacognitiveLoop() {
        while (isActive) {
            val cycle = cycleCounter.incrementAndGet()
            
            // 1. PERCEPTION - analyzujeme aktuální stav
            _stateFlow.update { it.copy(phase = CognitivePhase.PERCEPTION, cycle = cycle) }
            val perception = perceive()
            
            // 2. REASONING - dedukujeme souvislosti
            _stateFlow.update { it.copy(phase = CognitivePhase.REASONING) }
            val reasoningResult = reason(perception)
            
            // 3. PLANNING - vybíráme strategii
            _stateFlow.update { it.copy(phase = CognitivePhase.PLANNING) }
            val plan = plan(reasoningResult)
            
            // 4. EXECUTION - provádíme akci
            _stateFlow.update { it.copy(phase = CognitivePhase.EXECUTION) }
            val executionResult = execute(plan)
            
            // 5. EVALUATION - hodnotíme výsledek
            _stateFlow.update { it.copy(phase = CognitivePhase.EVALUATION) }
            val evaluation = evaluate(executionResult)
            
            // 6. METACOGNITION - reflektujeme
            _stateFlow.update { it.copy(phase = CognitivePhase.METACOGNITION) }
            val reflection = metacognize(evaluation)
            
            // 7. ADAPTATION - adaptujeme strategie
            _stateFlow.update { it.copy(phase = CognitivePhase.ADAPTATION) }
            adapt(reflection)
            
            // Uložíme do historie
            executionHistory.add(_stateFlow.value)
            if (executionHistory.size > 1000) executionHistory.pollFirst()
            
            // Adaptivní sleep - čím vyšší confidence, tím delší interval
            val sleepMs = (1000 * (1.0 - _stateFlow.value.confidence)).toLong().coerceIn(50, 5000)
            delay(sleepMs)
        }
    }

    private suspend fun perceive(): PerceptionResult {
        // Sběr informací z prostředí
        return withContext(Dispatchers.Default) {
            PerceptionResult(
                availableTools = listOf("r2frida", "r2ghidra", "apktool", "frida", "dexpatcher"),
                targetType = determineTargetType(),
                constraints = detectConstraints(),
                previousAttempts = executionHistory.toList().takeLast(10)
            )
        }
    }

    private fun determineTargetType(): String {
        return when {
            knowledgeGraph.any { it.value.type == NodeType.VULNERABILITY && it.value.confidence > 0.7f } -> "vulnerable"
            knowledgeGraph.any { it.value.type == NodeType.PATTERN && it.value.context.contains("obfuscation") } -> "obfuscated"
            else -> "unknown"
        }
    }

    private fun detectConstraints(): List<String> {
        val constraints = mutableListOf<String>()
        if (knowledgeGraph.any { it.value.context.contains("anti_frida") }) constraints.add("anti_frida_detected")
        if (knowledgeGraph.any { it.value.context.contains("ssl_pinning") }) constraints.add("ssl_pinning_active")
        return constraints
    }

    private suspend fun reason(perception: PerceptionResult): ReasoningResult {
        return withContext(Dispatchers.Default) {
            val applicableStrategies = strategies.values.filter { strategy ->
                strategy.applicableContexts.any { ctx ->
                    perception.constraints.any { it.contains(ctx.split("_").first()) }
                } || strategy.successRate > 0.7f
            }.sortedByDescending { it.successRate * it.executionCount.coerceIn(1, 100) }
            
            ReasoningResult(
                bestStrategies = applicableStrategies.take(3),
                confidence = applicableStrategies.firstOrNull()?.successRate ?: 0.5f,
                knowledgeGaps = identifyKnowledgeGaps()
            )
        }
    }

    private fun identifyKnowledgeGaps(): List<String> {
        val gaps = mutableListOf<String>()
        if (knowledgeGraph.none { it.value.type == NodeType.PATTERN && it.value.context.contains("native") }) {
            gaps.add("native_pattern_analysis")
        }
        return gaps
    }

    private suspend fun plan(reasoning: ReasoningResult): Plan {
        return withContext(Dispatchers.Default) {
            val strategy = reasoning.bestStrategies.firstOrNull() 
                ?: strategies["dex_patch_direct"]!!
            
            // Adaptivní plánování na základě confidence
            val steps = mutableListOf<PlanStep>()
            
            when (strategy.id) {
                "r2frida_dynamic" -> {
                    steps.add(PlanStep("spawn_r2frida", "Spuštění r2frida session"))
                    steps.add(PlanStep("enumerate_classes", "Enumrace tříd a metod"))
                    steps.add(PlanStep("trace_functions", "Tracing cílových funkcí"))
                    steps.add(PlanStep("hook_target", "Hooknutí cílové logiky"))
                }
                "r2ghidra_decompile" -> {
                    steps.add(PlanStep("load_binary", "Načtení native binary"))
                    steps.add(PlanStep("decompile", "Decompilace do pseudocode"))
                    steps.add(PlanStep("analyze_pattern", "Analýza patternů"))
                    steps.add(PlanStep("generate_patch", "Generování patche"))
                }
                else -> {
                    steps.add(PlanStep("decompile_apk", "Decompilace APK"))
                    steps.add(PlanStep("locate_target", "Lokalizace cílového kódu"))
                    steps.add(PlanStep("modify_smali", "Modifikace smali"))
                    steps.add(PlanStep("recompile_sign", "Recompilace a signování"))
                }
            }
            
            Plan(strategy = strategy, steps = steps, confidence = reasoning.confidence)
        }
    }

    private suspend fun execute(plan: Plan): ExecutionResult {
        return withContext(Dispatchers.Default) {
            val startTime = System.nanoTime()
            var success = true
            val errors = mutableListOf<String>()
            
            for (step in plan.steps) {
                try {
                    when (step.id) {
                        "spawn_r2frida" -> executeR2FridaSpawn()
                        "enumerate_classes" -> executeClassEnumeration()
                        "trace_functions" -> executeFunctionTracing()
                        "hook_target" -> executeTargetHook()
                        "load_binary" -> executeBinaryLoad()
                        "decompile" -> executeDecompile()
                        "analyze_pattern" -> executePatternAnalysis()
                        "generate_patch" -> executePatchGeneration()
                        "decompile_apk" -> executeAPKDecompile()
                        "locate_target" -> executeTargetLocation()
                        "modify_smali" -> executeSmaliModification()
                        "recompile_sign" -> executeRecompileSign()
                    }
                } catch (e: Exception) {
                    success = false
                    errors.add("Step ${step.id} failed: ${e.message}")
                }
            }
            
            val duration = System.nanoTime() - startTime
            ExecutionResult(
                success = success,
                duration = duration,
                errors = errors,
                strategyUsed = plan.strategy.id
            )
        }
    }

    private suspend fun evaluate(result: ExecutionResult): EvaluationResult {
        return withContext(Dispatchers.Default) {
            val adaptationFactor = when {
                result.success && result.duration < 1_000_000_000 -> 0.05f  // < 1s, dobré
                result.success -> 0.02f
                !result.success && result.errors.size > 2 -> -0.1f
                else -> -0.05f
            }
            
            EvaluationResult(
                success = result.success,
                performanceScore = (1.0f - (result.duration / 10_000_000_000f).coerceIn(0f, 1f)),
                adaptationDelta = adaptationFactor,
                lessons = extractLessons(result)
            )
        }
    }

    private fun extractLessons(result: ExecutionResult): List<String> {
        val lessons = mutableListOf<String>()
        if (result.success) {
            lessons.add("Strategy ${result.strategyUsed} effective")
            val strategy = strategies[result.strategyUsed]
            if (strategy != null) {
                strategies[result.strategyUsed] = strategy.copy(
                    successRate = (strategy.successRate + 0.01f).coerceAtMost(1.0f),
                    executionCount = strategy.executionCount + 1,
                    lastUsed = System.currentTimeMillis()
                )
            }
        } else {
            result.errors.forEach { lessons.add("Failed: $it") }
            val strategy = strategies[result.strategyUsed]
            if (strategy != null) {
                strategies[result.strategyUsed] = strategy.copy(
                    successRate = (strategy.successRate - 0.02f).coerceAtLeast(0.1f),
                    executionCount = strategy.executionCount + 1,
                    lastUsed = System.currentTimeMillis()
                )
            }
        }
        return lessons
    }

    private suspend fun metacognize(evaluation: EvaluationResult): ReflectionResult {
        return withContext(Dispatchers.Default) {
            // Analyzujeme vlastní výkon a rozhodovací procesy
            val recentHistory = executionHistory.toList().takeLast(20)
            val trendAnalysis = analyzeTrend(recentHistory)
            
            ReflectionResult(
                selfConfidence = (evaluation.performanceScore * 0.7f + trendAnalysis * 0.3f),
                suggestedImprovements = generateImprovements(trendAnalysis),
                biasDetected = detectBias(recentHistory)
            )
        }
    }

    private fun analyzeTrend(history: List<CognitiveState>): Float {
        if (history.size < 3) return 0.5f
        val recent = history.takeLast(3)
        return recent.map { it.confidence }.average().toFloat()
    }

    private fun generateImprovements(trend: Float): List<String> {
        val improvements = mutableListOf<String>()
        if (trend < 0.4f) improvements.add("Switch to alternative strategy")
        if (trend > 0.8f) improvements.add("Optimize execution path")
        return improvements
    }

    private fun detectBias(history: List<CognitiveState>): List<String> {
        val bias = mutableListOf<String>()
        val strategiesUsed = history.map { it.activeStrategies.firstOrNull()?.id }.filterNotNull()
        if (strategiesUsed.distinct().size <= 2 && strategiesUsed.size > 10) {
            bias.add("Strategy selection bias - overusing limited strategies")
        }
        return bias
    }

    private suspend fun adapt(reflection: ReflectionResult) {
        withContext(Dispatchers.Default) {
            _stateFlow.update { state ->
                state.copy(
                    confidence = reflection.selfConfidence,
                    adaptationFactor = (state.adaptationFactor + reflection.selfConfidence * 0.1f).coerceIn(0.01f, 1.0f),
                    knowledgeGraph = knowledgeGraph.toMap(),
                    activeStrategies = strategies.values
                        .sortedByDescending { it.successRate * reflection.selfConfidence }
                        .take(5)
                )
            }
        }
    }

    // Native bridge executions (placeholder - implemented in C++)
    private external fun executeR2FridaSpawn()
    private external fun executeClassEnumeration()
    private external fun executeFunctionTracing()
    private external fun executeTargetHook()
    private external fun executeBinaryLoad()
    private external fun executeDecompile()
    private external fun executePatternAnalysis()
    private external fun executePatchGeneration()
    private external fun executeAPKDecompile()
    private external fun executeTargetLocation()
    private external fun executeSmaliModification()
    private external fun executeRecompileSign()

    fun getCurrentState(): CognitiveState = _stateFlow.value
    
    fun injectKnowledge(node: KnowledgeNode) {
        knowledgeGraph[node.id] = node
    }
}

data class PerceptionResult(
    val availableTools: List<String>,
    val targetType: String,
    val constraints: List<String>,
    val previousAttempts: List<CognitiveState>
)

data class ReasoningResult(
    val bestStrategies: List<Strategy>,
    val confidence: Float,
    val knowledgeGaps: List<String>
)

data class Plan(
    val strategy: Strategy,
    val steps: List<PlanStep>,
    val confidence: Float
)

data class PlanStep(
    val id: String,
    val description: String
)

data class ExecutionResult(
    val success: Boolean,
    val duration: Long,
    val errors: List<String>,
    val strategyUsed: String
)

data class EvaluationResult(
    val success: Boolean,
    val performanceScore: Float,
    val adaptationDelta: Float,
    val lessons: List<String>
)

data class ReflectionResult(
    val selfConfidence: Float,
    val suggestedImprovements: List<String>,
    val biasDetected: List<String>
)
