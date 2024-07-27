package data.scripts.campaign.magnetar

import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.WeaponAPI.AIHints
import com.fs.starfarer.api.loading.WeaponSlotAPI
import com.fs.starfarer.api.loading.WeaponSpecAPI
import com.fs.starfarer.api.plugins.AutofitPlugin.AutofitPluginDelegate
import com.fs.starfarer.api.plugins.AutofitPlugin.AvailableWeapon
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.MathUtils
import java.util.*

class MPC_derelictOmegaDerelictAutofitPlugin(fleetCommander: PersonAPI?) : CoreAutofitPlugin(fleetCommander) {

    companion object {
        const val CHANCE_FOR_OMEGA_TO_BE_BOOSTED = 0.4f
    }

    override fun getBestMatch(
        desired: WeaponSpecAPI, useBetter: Boolean,
        catId: String?, alreadyUsed: Set<String?>, possible: List<AvailableWeapon>,
        slot: WeaponSlotAPI?,
        delegate: AutofitPluginDelegate
    ): AvailableWeapon? {
        //AvailableWeapon best = null;
        var bestScore = -1f
        var bestIsPriority = false
        var bestSize = -1
        val cat = categories[catId] ?: return null
        val desiredTag = getCategoryTag(cat, desired.tags)
        var desiredLevel = getLevel(desiredTag)
        if (desiredTag == null) {
            // fallback to categories that aren't in the tags of the desired weapon
//			for (String tag : desired.getTags()) {
//				desiredLevel = Math.max(desiredLevel, getLevel(tag));
//			}
            desiredLevel = 10000f
        }
        val longRange = desired.hasTag(LR)
        val shortRange = desired.hasTag(SR)
        val midRange = !longRange && !shortRange
        val desiredPD = desired.aiHints.contains(AIHints.PD)
        val best = WeightedRandomPicker<AvailableWeapon>(random)


//		boolean randomize = isChecked(RANDOMIZE);
//		if (randomize) {
//			shortRange = true;
//			longRange = false;
//			midRange = !longRange && !shortRange;
//			desiredPD = true;
//		}
        var iter = 0
        for (w in possible) {
            iter++
            val spec = w.spec
            if (w.spec.tags.contains("omega")) {
                if (MathUtils.getRandom().nextFloat() <= CHANCE_FOR_OMEGA_TO_BE_BOOSTED) {
                    return w
                }
            }
            val catTag = getCategoryTag(cat, spec.tags) ?: continue
            // not in this category

//			if (desired.getWeaponId().equals("autopulse") && spec.getWeaponId().contains("phase")) {
//				System.out.println("wefwefwe");
//			}
            val currLongRange = spec.hasTag(LR)
            val currShortRange = spec.hasTag(SR)
            val currMidRange = !currLongRange && !currShortRange

            // don't fit short-range weapons instead of long-range ones unless it's PD
            if (!desiredPD && currShortRange && (midRange || longRange)) continue
            //if (currMidRange && longRange) continue;
            val isPrimaryCategory = cat.base == spec.autofitCategory
            val currIsPriority = isPrimaryCategory && delegate.isPriority(spec)
            val currSize = spec.size.ordinal
            val betterDueToPriority = currSize >= bestSize && currIsPriority && !bestIsPriority
            val worseDueToPriority = currSize <= bestSize && !currIsPriority && bestIsPriority
            if (worseDueToPriority) continue
            var level = getLevel(catTag)
            //if (randomize) level += random.nextInt(20);
            if (!randomize && !useBetter && !betterDueToPriority && (level > desiredLevel)) continue
            var rMag = 0
            if (randomize && desired.size == spec.size) {
                rMag = 20
            } else if (desired.size == spec.size) {
                //if (delegate.getFaction() != null && delegate.getFaction().getDoctrine().getAutofitRandomizeProbability() > 0) {
                if (delegate.isAllowSlightRandomization) {
                    rMag = 4
                }
            }
            if (rMag > 0) {
                val symmetric = random.nextFloat() < 0.75f
                level += if (slot != null && symmetric) {
                    val seed = Math.abs((slot.location.x / 2f).toInt()) * 723489413945245311L xor 1181783497276652981L
                    val r = Random((seed + weaponFilterSeed) * iter)
                    r.nextInt(rMag).toFloat()
                } else {
                    random.nextInt(rMag).toFloat()
                }
            }
            val score = level
            //			if (delegate.isPriority(spec)) {
//				score += PRIORITY;
//			}
            if (score > bestScore || betterDueToPriority) {
                //best = w;
                best.clear()
                best.add(w)
                bestScore = score
                bestSize = currSize
                bestIsPriority = currIsPriority
            } else if (score == bestScore) {
                best.add(w)
            }
        }
        //		if (desired.getWeaponId().equals("autopulse")) {
//			System.out.println("wefwefwe");
//		}


        // if the best-match tier includes the weapon specified in the target variant, use that
        // prefer one we already have to buying
        val allMatches: MutableList<AvailableWeapon> = ArrayList()
        val freeMatches: MutableList<AvailableWeapon> = ArrayList()
        for (w in best.items) {
            if (desired.weaponId == w.id) {
                allMatches.add(w)
                if (w.price <= 0) {
                    freeMatches.add(w)
                }
            }
        }
        if (!freeMatches.isEmpty()) return freeMatches[0]
        if (!allMatches.isEmpty()) return allMatches[0]

        // if the best-match tier includes a weapon that we already own, filter out all non-free ones
        var hasFree = false
        var hasNonBlackMarket = false
        for (w in best.items) {
            if (w.price <= 0) {
                hasFree = true
            }
            if (w.submarket == null || !w.submarket.plugin.isBlackMarket) {
                hasNonBlackMarket = true
            }
        }
        if (hasFree) {
            for (w in ArrayList(best.items)) {
                if (w.price > 0) {
                    best.remove(w)
                }
            }
        } else if (hasNonBlackMarket) {
            for (w in ArrayList(best.items)) {
                if (w.submarket != null && w.submarket.plugin.isBlackMarket) {
                    best.remove(w)
                }
            }
        }

        // if the best-match tier includes a weapon we used already, use that
        if (!alreadyUsed.isEmpty()) {
            for (w in best.items) {
                if (alreadyUsed.contains(w.id)) return w
            }
        }
        return if (best.isEmpty) null else best.pick()

        //return best.getItems().get(0);
    }
}