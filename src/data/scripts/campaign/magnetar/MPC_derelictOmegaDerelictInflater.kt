package data.scripts.campaign.magnetar

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.impl.campaign.DModManager
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.loading.VariantSource
import com.fs.starfarer.api.plugins.AutofitPlugin.AvailableFighter
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin
import com.fs.starfarer.api.util.ListMap
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import java.util.*

class MPC_derelictOmegaDerelictInflater(p: DefaultFleetInflaterParams?): DefaultFleetInflater(p) {
    override fun inflate(fleet: CampaignFleetAPI) {
        var random = Random()
        //p.seed = null;
        if (p.seed != null) random = Random(p.seed)

        //p.quality = 2f;

        //random = new Random();
        var dmodRandom: Random? = Random()
        if (p.seed != null) dmodRandom = Misc.getRandom(p.seed, 5)
        //val auto = CoreAutofitPlugin(fleet.commander) // NOTICE: EDIT
        val auto = MPC_derelictOmegaDerelictAutofitPlugin(fleet.commander)
        auto.random = random
        val upgrade = random.nextFloat() < Math.min(0.1f + p.quality * 0.5f, 0.5f)
        auto.setChecked(CoreAutofitPlugin.UPGRADE, upgrade)

        //auto.setChecked(CoreAutofitPlugin.RANDOMIZE, true);
        //auto.getOptions().get(4).checked = true; // upgrade
        this.fleet = fleet
        faction = fleet.faction
        if (p.factionId != null) {
            faction = Global.getSector().getFaction(p.factionId)
        }

        //this.faction = Global.getSector().getFaction(Factions.HEGEMONY);
        hullmods = ArrayList(faction.knownHullMods)

//		fighters = new ArrayList<AvailableFighter>();
//		for (String wingId : faction.getKnownFighters()) {
//			fighters.add(new AvailableFighterImpl(wingId, 1000));
//		}
        val nonPriorityWeapons = SortedWeapons()
        val priorityWeapons = SortedWeapons()
        val weaponCategories: MutableSet<String?> = LinkedHashSet()
        for (weaponId in faction.knownWeapons) {
            if (!faction.isWeaponKnownAt(weaponId, p.timestamp)) continue
            val spec = Global.getSettings().getWeaponSpec(weaponId)
                ?: throw RuntimeException("Weapon with spec id [$weaponId] not found")
            //if (mode == ShipPickMode.IMPORTED && !spec.hasTag(Items.TAG_BASE_BP)) continue;
            val tier = spec.tier
            val cat = spec.autofitCategory
            if (isPriority(spec)) {
                val list = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.size)
                list.add(AvailableWeaponImpl(spec, 1000))
            } else {
                val list = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.size)
                list.add(AvailableWeaponImpl(spec, 1000))
            }
            weaponCategories.add(cat)
        }
        val nonPriorityFighters = ListMap<AvailableFighter>()
        val priorityFighters = ListMap<AvailableFighter>()
        val fighterCategories: MutableSet<String> = LinkedHashSet()
        for (wingId in faction.knownFighters) {
            if (!faction.isFighterKnownAt(wingId, p.timestamp)) continue
            val spec = Global.getSettings().getFighterWingSpec(wingId)
                ?: throw RuntimeException("Fighter wing with spec id [$wingId] not found")

            //if (mode == ShipPickMode.IMPORTED && !spec.hasTag(Items.TAG_BASE_BP)) continue;
            //int tier = spec.getTier();
            val cat = spec.autofitCategory
            //			if (cat == null) {
//				System.out.println("wfewfwe");
//			}
            if (isPriority(spec)) {
                priorityFighters.add(cat, AvailableFighterImpl(spec, 1000))
            } else {
                nonPriorityFighters.add(cat, AvailableFighterImpl(spec, 1000))
            }
            fighterCategories.add(cat)
        }


        //float averageDmods = (1f - quality) / Global.getSettings().getFloat("qualityPerDMod");
        val averageDmods = getAverageDmodsForQuality(p.quality)

        //System.out.println("Quality: " + quality + ", Average: " + averageDmods);
        val forceAutofit = fleet.memoryWithoutUpdate.getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS)
        var memberIndex = 0
        for (member in fleet.fleetData.membersListCopy) {
            if (!forceAutofit && member.hullSpec.hasTag(Tags.TAG_NO_AUTOFIT)) {
                continue
            }
            if (!forceAutofit && (member.variant != null) && member.variant.hasTag(Tags.TAG_NO_AUTOFIT)) {
                continue
            }
            if (!faction.isPlayerFaction) {
                if (!forceAutofit && member.hullSpec.hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
                    continue
                }
                if (!forceAutofit && member.variant != null && member.variant.hasTag(Tags.TAG_NO_AUTOFIT_UNLESS_PLAYER)) {
                    continue
                }
            }

            // need this so that when reinflating a fleet that lost members, the members reinflate consistently
            if (p.seed != null) {
                val extra = member.shipName.hashCode()
                random = Random(p.seed * extra)
                auto.random = random
                dmodRandom = Misc.getRandom(p.seed * extra, 5)
            }
            val sizes: MutableList<WeaponSize> = ArrayList()
            sizes.add(WeaponSize.SMALL)
            sizes.add(WeaponSize.MEDIUM)
            sizes.add(WeaponSize.LARGE)
            weapons = ArrayList()
            for (cat in weaponCategories) {
                for (size in sizes) {
                    var foundSome = false
                    for (tier in 0..3) {
                        var p = getTierProbability(tier, p.quality)
                        if (this.p.allWeapons != null && this.p.allWeapons) {
                            p = 1f
                        }
                        val priority = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size)
                        val nonPriority = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size)
                        if (!foundSome) {
                            p = 1f
                        }
                        val tierAvailable = random.nextFloat() < p
                        if (!tierAvailable && foundSome) continue
                        //if (random.nextFloat() >= p) continue;
                        var num = 2
                        num = when (size) {
                            WeaponSize.LARGE -> 2
                            WeaponSize.MEDIUM -> 2
                            WeaponSize.SMALL -> 2
                        }
                        //						if (!tierAvailable) {
//							num = 1;
//						}
                        if (this.p.allWeapons != null && this.p.allWeapons) {
                            num = 500
                        }
                        var picks = makePicks(num, priority.size, random)
                        for (index in picks) {
                            val w = priority[index!!]
                            weapons.add(w)
                            foundSome = true
                        }
                        num -= picks.size
                        if (num > 0) {
                            picks = makePicks(num, nonPriority.size, random)
                            for (index in picks) {
                                val w = nonPriority[index!!]
                                weapons.add(w)
                                foundSome = true
                            }
                        }
                    }
                }
            }
            fighters = ArrayList()
            for (cat in fighterCategories) {
                val priority = priorityFighters[cat]
                var madePriorityPicks = false
                if (priority != null) {
                    var num = random.nextInt(2) + 1
                    if (p.allWeapons != null && p.allWeapons) {
                        num = 100
                    }
                    val picks = makePicks(num, priority.size, random)
                    for (index in picks) {
                        val f = priority[index!!]
                        fighters.add(f)
                        madePriorityPicks = true
                    }
                }
                if (!madePriorityPicks) {
                    var num = random.nextInt(2) + 1
                    if (p.allWeapons != null && p.allWeapons) {
                        num = 100
                    }
                    val nonPriority = nonPriorityFighters[cat]
                    val picks = makePicks(num, nonPriority!!.size, random)
                    for (index in picks) {
                        val f = nonPriority[index!!]
                        fighters.add(f)
                    }
                }
            }
            var target = member.variant
            if (target.originalVariant != null) {
                // needed if inflating the same fleet repeatedly to pick up weapon availability changes etc
                target = Global.getSettings().getVariant(target.originalVariant)
            }
            if (faction.isPlayerFaction) {
                if (random.nextFloat() < GOAL_VARIANT_PROBABILITY) {
                    val targets = Global.getSector().autofitVariants.getTargetVariants(member.hullId)
                    val alts = WeightedRandomPicker<ShipVariantAPI>(random)
                    for (curr in targets) {
                        if (curr.hullSpec.hullId == target.hullSpec.hullId) {
                            alts.add(curr)
                        }
                    }
                    if (!alts.isEmpty) {
                        target = alts.pick()
                    }
                }
            }
            currVariant = Global.getSettings().createEmptyVariant(fleet.id + "_" + memberIndex, target.hullSpec)
            currMember = member
            if (target.isStockVariant) {
                currVariant.originalVariant = target.hullVariantId
            }
            var rProb = faction.doctrine.autofitRandomizeProbability
            if (p.rProb != null) rProb = p.rProb
            var randomize = random.nextFloat() < rProb
            if (member.isStation) randomize = false
            auto.setChecked(CoreAutofitPlugin.RANDOMIZE, randomize)
            memberIndex++
            var maxSmods = 0
            if (p.averageSMods != null && !member.isCivilian) {
                maxSmods = getMaxSMods(currVariant, p.averageSMods, dmodRandom) - currVariant.sMods.size
            }
            auto.doFit(currVariant, target, maxSmods, this)
            currVariant.source = VariantSource.REFIT
            member.setVariant(currVariant, false, false)

            //int dmods = (int) Math.round(averageDmods + dmodRandom.nextFloat() * 2f - 1f);
//			int dmods = (int) Math.round(averageDmods + dmodRandom.nextFloat() * 3f - 2f);
//			if (dmods > 5) dmods = 5;
//			int dmodsAlready = DModManager.getNumDMods(currVariant);
//			dmods -= dmodsAlready;
//			if (dmods > 0) {
//				DModManager.setDHull(currVariant);
//				DModManager.addDMods(member, true, dmods, dmodRandom);
//			}
            if (!currMember.isStation) {
                val addDmods = getNumDModsToAdd(currVariant, averageDmods, dmodRandom)
                if (addDmods > 0) {
                    DModManager.setDHull(currVariant)
                    DModManager.addDMods(member, true, addDmods, dmodRandom)
                }
            }
        }
        fleet.fleetData.setSyncNeeded()
        fleet.fleetData.syncIfNeeded()

        // handled in the method that calls inflate()
        //ListenerUtil.reportFleetInflated(fleet, this);
    }
}