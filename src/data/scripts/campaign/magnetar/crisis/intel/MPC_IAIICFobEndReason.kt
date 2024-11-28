package data.scripts.campaign.magnetar.crisis.intel

enum class MPC_IAIICFobEndReason(
    val consideredVictory: Boolean = false
) {
    FRACTAL_CORE_OBTAINED,
    FRACTAL_COLONY_LOST,
    TOTAL_FAILURE(true),
}