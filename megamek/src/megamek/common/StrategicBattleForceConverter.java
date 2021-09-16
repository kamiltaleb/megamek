package megamek.common;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;

import megamek.MegaMek;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import static megamek.common.BattleForceSPA.*;
import static megamek.common.SBFElementType.*;

public final class StrategicBattleForceConverter {

    public static SBFFormation createSbfFormationFromAS(
            List<AlphaStrikeElement> firstUnit, List<AlphaStrikeElement>... furtherUnits) {
        var result = new SBFFormation();
        result.getUnits().add(createSbfUnit(firstUnit, "Unit 1"));
        int unitCount = 2;
        for (Collection<AlphaStrikeElement> furtherUnit : furtherUnits) {
            result.getUnits().add(createSbfUnit(furtherUnit, "Unit " + unitCount++));
        }
        calcSbfFormationStats(result, "Test");
        
        return result;
    }
    
    public static SBFFormation createSbfFormationFromAS(List<List<AlphaStrikeElement>> units) {
        var result = new SBFFormation();
        int unitCount = 1;
        for (List<AlphaStrikeElement> furtherUnit : units) {
            result.getUnits().add(createSbfUnit(furtherUnit, "Unit " + unitCount++));
        }
        calcSbfFormationStats(result, "Test");
        return result;
    }

    public static SBFFormation createSbfFormationFromTW(List<Entity> firstUnit, List<Entity>... furtherUnits) {
        List<List<AlphaStrikeElement>> asUnits = new ArrayList<>();
        List<AlphaStrikeElement> currentAsUnit = new ArrayList<>(); 
        asUnits.add(currentAsUnit);
        for (Entity entity : firstUnit) {
            currentAsUnit.add(AlphaStrikeConverter.convertToAlphaStrike(entity));
        }
        for (Collection<Entity> furtherUnit : furtherUnits) {
            currentAsUnit = new ArrayList<>(); 
            asUnits.add(currentAsUnit);
            for (Entity entity : furtherUnit) {
                currentAsUnit.add(AlphaStrikeConverter.convertToAlphaStrike(entity));
            }
        }
        return createSbfFormationFromAS(asUnits);
    }

    public static SBFFormation createSbfFormationFromForce(Force force, IGame game) {
        if (!canConvertToSbfUnit(force, game)) {
            return null;
        }
        var result = new SBFFormation();
        Forces forces = game.getForces();
        for (Force subforce : forces.getFullSubForces(force)) {
            var thisUnit = new ArrayList<AlphaStrikeElement>();
            for (Entity entity : forces.getFullEntities(subforce)) {
                thisUnit.add(AlphaStrikeConverter.convertToAlphaStrike(entity));
            }
            result.getUnits().add(createSbfUnit(thisUnit, subforce.getName()));
        }
        calcSbfFormationStats(result, force.getName());
        return result;
    }

    public static void calcSbfFormationStats(SBFFormation result, String name) {
        if (!canConvertToSbfFormation(result.getUnits())) {
            return;
        }

        result.setName(name);
        result.setType(calcFormationType(result.getUnits()));
        result.setSize((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getSize).average().orElse(0)));
        result.setMovement((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getMovement).average().orElse(0)));

        result.setTrspMovement((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getTrspMovement).average().orElse(0)));
        result.setJumpMove((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getJumpMove).average().orElse(0)));
        result.setTmm((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getTmm).average().orElse(0)));
        result.setSkill((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getSkill).average().orElse(0)));
        calcFormationSpecialAbilities(result);
        result.setTactics(getFormationTactics(result));
        result.setMorale(3 + result.getSkill());
        result.setPointValue(result.getUnits().stream().mapToInt(SBFUnit::getPointValue).sum());
    }

    public static SBFUnit createSbfUnit(Collection<AlphaStrikeElement> elements, String name) {
        if (!canConvertToSbfUnit(elements)) {
            return null;
        }
        var result = new SBFUnit();
        result.setName(name);
        result.setType(getUnitType(elements));
        result.setSize(getUnitSize(elements));
        result.setArmor(calcUnitArmor(elements));
        result.setMovement(calcUnitMove(elements));
        result.setJumpMove(getUnitJumpMove(elements));
        result.setTmm(getUnitTMM(elements, result));
        calcUnitSpecialAbilities(elements, result);
        result.setDamage(calcUnitDamage(elements, result));
        result.setPointValue(getUnitPointValue(elements));
        result.setSkill(4);
        return result;
    }



    public static boolean canConvertToSbfFormation(Collection<SBFUnit> units) {
        if (units.isEmpty() || units.size() > 4) {
            return false;
        }
        return true;
    }

    public static boolean canConvertToSbfUnit(Collection<AlphaStrikeElement> elements) {
        //TODO
        return true;
    }

    public static boolean canConvertToSbfUnit(Force force, IGame game) {
        Forces forces = game.getForces();
        if (force.isEmpty()) {
            return false;
        }
        if (force.getSubForces().isEmpty()) {
            return false;
        }
        if (forces.getFullEntities(force).size() > 20) {
            return false;
        }
        //TODO
        return true;
    }

    private static SBFElementType getUnitType(Collection<AlphaStrikeElement> elements) {
        if (elements.isEmpty()) {
            MegaMek.getLogger().error("Cannot determine SBF Element Type for an empty list of AS Elements.");
            return null;
        }
        int majority = (int) Math.round(2.0 / 3 * elements.size());
        List<SBFElementType> types = elements.stream().map(SBFElementType::getUnitType).collect(toList());
        Map<SBFElementType, Long> frequencies = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestOccurrence = frequencies.values().stream().max(Long::compare).orElseGet(() -> 0l);




//        int highestOccurrence = types.stream()
//                .collect(groupingBy(Function.identity(), counting()))
//                .entrySet()
//                .stream()
//                .max(Map.Entry.comparingByValue())
//                .get()
//                .getValue()
//                .intValue();


        if (highestOccurrence < majority) {
            return SBFElementType.MX;
        } else {
            return types.stream()
                    .filter(e -> Collections.frequency(types, e) == highestOccurrence)
                    .findFirst().get();
        }
    }

    private static int getUnitSize(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(e -> e.getSize()).average().orElse(0));
    }

    private static int getUnitTMM(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        int avgMove = (int)Math.round(elements.stream()
                .mapToInt(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0)) / 2;
        int result = getTmmFromMove(avgMove);
        result += (unit.getType() == BA) || (unit.getType() == PM) ? 1 : 0;
        // TODO: movement type?
        return result;
    }

    private static ASDamageVector calcUnitDamage(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        double dmgS = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.S)).sum();
        double dmgM = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.M)).sum();
        double dmgL = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.L)).sum();
        double dmgE = elements.stream().map(AlphaStrikeElement::getStandardDamage).mapToDouble(d -> sbfDamage(d.E)).sum();
        dmgS += elements.stream().mapToDouble(AlphaStrikeElement::getOverheat).sum() / 2;
        dmgS += unit.isAnyTypeOf(BA, CI) && unit.hasSPA(AM) ? 1 : 0;
        dmgM += elements.stream().filter(e -> e.getStandardDamage().M.damage >= 1).mapToDouble(AlphaStrikeElement::getOverheat).sum() / 2;
        dmgL += elements.stream().filter(e -> e.getStandardDamage().L.damage >= 1).mapToDouble(AlphaStrikeElement::getOverheat).sum() / 2;

        if (unit.getType() == AS) {
            return ASDamageVector.createStandardDamage(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3), Math.round(dmgE / 3));
        } else {
            return ASDamageVector.createStandardDamage(Math.round(dmgS / 3), Math.round(dmgM / 3),
                    Math.round(dmgL / 3));
        }
    }

    private static void calcUnitSpecialAbilities(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        addUnitSpasIfAny(elements, unit, PRB, AECM, BHJ2, BHJ3, BH, BT, ECM, HPG, LPRB, LECM, TAG);
        addUnitSpasIfHalf(elements, unit, AMS, ARM, ARS, BAR, BFC, CR, ENG, RCN, RBT, SRCH, SHLD);
        addUnitSpasIfAll(elements, unit, AMP, AM, BHJ, XMEC, MCS, UCS, MEC, PARA, SAW, TRN);
        sumUnitSpas(elements, unit, OMNI, CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, DT, MT, PT, ST, SCR, PNT);
        sumUnitSpasDivideBy3(elements, unit, ATAC, BOMB, IF);
        calcUnitMhq(elements, unit);


        if (((spaCount(elements, C3M) >= 1) || (spaCount(elements, C3BSM) >= 1))
                && (spaCount(elements, C3M) + spaCount(elements, C3S) + spaCount(elements, C3BSS) >= elements.size() / 2)) {
            unit.addSPA(AC3);
        }

        if (spaCount(elements, C3I) >= elements.size() / 2) {
            unit.addSPA(AC3);
        }

        if (unit.hasSPA(ATAC)) {
            unit.replaceSPA(ATAC, Math.round(1.0d / 3 * (int) unit.getSPA(ATAC)));
        }


    }

    private static void calcFormationSpecialAbilities(SBFFormation formation) {
        addFormationSpasIfAny(formation, DN, XMEC, COM, HPG, LEAD, MCS, UCS, MEC, MAS, LMAS,
                MSW, MFB, SAW, SDS, TRN, FD, HELI, SDCS);
        addFormationSpasIf2Thirds(formation, AC3, PRB, AECM, ECM, ENG, LPRB, LECM, ORO, RCN, SRCH, SHLD, TAG, WAT);
        addFormationSpasIfAll(formation, AMP, BH, EE, FC, SEAL, MAG, PARA, RAIL, RBT, UMU);
        sumFormationSpas(formation, OMNI, CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, BOMB, DT, MT, PT, ST, SCR, PNT, IF, MHQ);

    }

    /**
     * Returns the number of the given AlphaStrike elements that have the given spa (regardless of its
     * associated objects)
     */
    private static int spaCount(Collection<AlphaStrikeElement> elements, BattleForceSPA spa) {
        return (int)elements.stream().filter(e -> e.hasSPA(spa)).count();
    }

    /**
     * Returns the number of the given SBFUnits that have the given spa (regardless of its
     * associated objects)
     */
    private static int spaCount(SBFFormation formation, BattleForceSPA spa) {
        return (int)formation.getUnits().stream().filter(e -> e.hasSPA(spa)).count();
    }

    private static void addUnitSpasIfAny(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSPA... spas) {
        addUnitSpas(elements, unit, 1, spas);
    }

    private static void addUnitSpasIfHalf(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSPA... spas) {
        addUnitSpas(elements, unit, elements.size() / 2, spas);
    }

    private static void addUnitSpasIfAll(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSPA... spas) {
        addUnitSpas(elements, unit, elements.size(), spas);
    }

    private static void addUnitSpas(Collection<AlphaStrikeElement> elements, SBFUnit unit,
                                    int threshold, BattleForceSPA[] spas) {
        for (BattleForceSPA spa : spas) {
            if (spaCount(elements, spa) >= threshold) {
                unit.addSPA(spa);
            }
        }
    }

    private static void sumUnitSpas(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSPA... spas) {
        for (BattleForceSPA spa : spas) {
            for (AlphaStrikeElement element : elements) {
                if (element.hasSPA(spa)) {
                    if (element.getSPA(spa) == null) {
                        unit.addSPA(spa, 1);
                    } else if (element.getSPA(spa) instanceof Integer) {
                        unit.addSPA(spa, (Integer) element.getSPA(spa));
                    } else if (element.getSPA(spa) instanceof Double) {
                        unit.addSPA(spa, (Double) element.getSPA(spa));
                    } else if (element.getSPA(spa) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSPA(spa)).getRangeBands() == 1) {
                        unit.addSPA(spa, sbfDamage(((ASDamageVector) element.getSPA(spa)).S));
                    }
                }
            }
        }
    }

    private static void sumUnitSpasDivideBy3(Collection<AlphaStrikeElement> elements, SBFUnit unit, BattleForceSPA... spas) {
        for (BattleForceSPA spa : spas) {
            for (AlphaStrikeElement element : elements) {
                if (element.hasSPA(spa)) {
                    if (element.getSPA(spa) == null) {
                        unit.addSPA(spa, 1);
                    } else if (element.getSPA(spa) instanceof Integer) {
                        unit.addSPA(spa, (Integer) element.getSPA(spa));
                    } else if (element.getSPA(spa) instanceof Double) {
                        unit.addSPA(spa, (Double) element.getSPA(spa));
                    } else if (element.getSPA(spa) instanceof ASDamageVector
                            && ((ASDamageVector)element.getSPA(spa)).getRangeBands() == 1) {
                        unit.addSPA(spa, sbfDamage(((ASDamageVector) element.getSPA(spa)).S));
                    }
                }
            }
            if (unit.hasSPA(spa)) {
                int oneThird = (int) Math.round(1.0d / 3 * (double) unit.getSPA(spa));
                if (oneThird == 0) {
                    unit.removeSPA(spa);
                } else {
                    unit.replaceSPA(spa, oneThird);
                }
            }
        }
    }

    private static void calcUnitMhq(Collection<AlphaStrikeElement> elements, SBFUnit unit) {
        double mhq = elements.stream().filter(e -> e.hasSPA(MHQ)).mapToInt(e -> Math.max(0, (int) e.getSPA(MHQ) - 1)).sum();
        int oneThird = (int) Math.round(mhq / 3);
        if (oneThird > 0) {
            unit.addSPA(MHQ, oneThird);
        }
    }

    private static void addFormationSpasIfAny(SBFFormation formation, BattleForceSPA... spas) {
        addFormationSpas(formation, 1, spas);
    }

    private static void addFormationSpasIfAll(SBFFormation formation, BattleForceSPA... spas) {
        addFormationSpas(formation, formation.getUnits().size(), spas);
    }

    private static void addFormationSpasIf2Thirds(SBFFormation formation, BattleForceSPA... spas) {
        int twoThirds = Math.max(formation.getUnits().size() - 1, 1);
        addFormationSpas(formation, twoThirds, spas);
    }

    private static void addFormationSpas(SBFFormation formation, int threshold, BattleForceSPA[] spas) {
        for (BattleForceSPA spa : spas) {
            if (spaCount(formation, spa) >= threshold) {
                formation.addSPA(spa);
            }
        }
    }

    private static void sumFormationSpas(SBFFormation formation, BattleForceSPA... spas) {
        for (BattleForceSPA spa : spas) {
            for (SBFUnit unit : formation.getUnits()) {
                if (unit.hasSPA(spa)) {
                    if (unit.getSPA(spa) == null) {
                        formation.addSPA(spa, 1);
                    } else if (unit.getSPA(spa) instanceof Integer) {
                        formation.addSPA(spa, (Integer) unit.getSPA(spa));
                    } else if (unit.getSPA(spa) instanceof Double) {
                        formation.addSPA(spa, (Double) unit.getSPA(spa));
                    }
                }
            }
        }
    }

    /** Returns the SBF damage of an AlphaStrike damage (0.5 for minimal damage, the AS damage otherwise). */
    private static double sbfDamage(ASDamage asDamage) {
        return asDamage.minimal ? 0.5 : asDamage.damage;
    }

    private static int getTmmFromMove(int move) {
        if (move >= 18) {
            return 5;
        } else if (move >= 10) {
            return 4;
        } else if (move >= 7) {
            return 3;
        } else if (move >= 5) {
            return 2;
        } else if (move >= 3) {
            return 1;
        } else if (move >= 1) {
            return 0;
        } else {
            return -4;
        }
    }

    private static int calcUnitMove(Collection<AlphaStrikeElement> elements) {
        int result = (int)Math.round(elements.stream().mapToDouble(AlphaStrikeElement::getPrimaryMovementValue).average().orElse(0) / 2);
        if (elements.stream().anyMatch(AlphaStrikeElement::isInfantry)) {
            int minInfantryMove = elements.stream().filter(AlphaStrikeElement::isInfantry).mapToInt(AlphaStrikeElement::getPrimaryMovementValue).min().orElse(0);
            result = Math.min(result, minInfantryMove);
        }
        return result;
    }

    private static int getUnitJumpMove(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round(elements.stream().mapToInt(AlphaStrikeElement::getJumpMove).average().orElse(0) / 4);
    }

    private static int calcUnitArmor(Collection<AlphaStrikeElement> elements) {
        double result = 0;
        for (AlphaStrikeElement element : elements) {
            result += element.getFinalArmor() + element.getStructure();
            result += (element.getStructure() >= 3 || element.hasAnySPAOf(AMS, CASE)) ? 0.5 : 0;
            result += (element.hasAnySPAOf(ENE, CASEII, CR, RAMS)) ? 1 : 0;
        }
        return (int)Math.round(result / 3);
    }
    
    private static int getUnitPointValue(Collection<AlphaStrikeElement> elements) {
        return (int) Math.round( 1.0d / 3 * elements.stream().mapToInt(BattleForceElement::getFinalPoints).sum());
    }
    
    private static int getFormationTactics(SBFFormation formation) {
        int tactics = Math.max(0, 10 - formation.getMovement() + formation.getSkill() - 4);
        if (formation.hasSPA(MHQ)) {
            double mhqValue = (Integer)formation.getSPA(MHQ);
            tactics -= Math.min(3, Math.round(mhqValue / 2));
        }
        return tactics;
    }

    private static SBFElementType calcFormationType(Collection<SBFUnit> units) {
        if (units.isEmpty()) {
            MegaMek.getLogger().error("Cannot determine SBF Formation Type for an empty list of SBF Units.");
            return null;
        }
        int majority = (int) Math.round(2.0 / 3 * units.size());
        List<SBFElementType> types = units.stream().map(SBFUnit::getType).collect(toList());
//        List<SBFElementType> types = elements.stream().map(SBFElementType::getUnitType).collect(toList());
        Map<SBFElementType, Long> frequencies = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestOccurrence = frequencies.values().stream().max(Long::compare).orElse(0l);
//        types.stream().forEach(System.out::println);
//        int highestOccurrence = types.stream()
//                .collect(groupingBy(Function.identity(), counting()))
//                .entrySet()
//                .stream()
//                .max(Map.Entry.comparingByValue())
//                .get().getValue().intValue();
        if (highestOccurrence < majority) {
            return SBFElementType.MX;
        } else {
            return types.stream()
                    .filter(e -> Collections.frequency(types, e) == highestOccurrence)
                    .findFirst().get();
        }
    }
}
