package data.scripts.campaign.magnetar.crisis.intel

enum class MPC_IAIICFobEndReason(
    /** If true, the IAIIC fob will be left behind, and the fractal core will be upgraded. */
    val consideredVictory: Boolean = false
) {
    FRACTAL_CORE_OBTAINED,
    FRACTAL_COLONY_LOST,
    FAILED_ALL_OUT_ATTACK(true),
    LOSS_OF_BENEFACTORS(true);
}