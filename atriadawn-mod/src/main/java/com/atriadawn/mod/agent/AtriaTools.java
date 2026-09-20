package com.atriadawn.mod.agent;

import com.atriadawn.mod.AtriaConfig;
import com.atriadawn.mod.entity.AtriaCompanionEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * «Инструменты» агента — то, что нейросеть может попросить сделать с телом
 * компаньона. Список спецификаций ({@link #buildToolSpecs()}) отправляется
 * модели в запросе (OpenAI-совместимый формат tools), а
 * {@link #execute} выполняет вызов на серверном потоке и возвращает
 * future со строкой-результатом, которая уходит модели следующим сообщением.
 */
public final class AtriaTools {

    /** Максимальная длительность ожидаемых действий, в тиках. */
    private static final int WALK_TIMEOUT = 20 * 45;
    private static final int MINE_TIMEOUT = 20 * 30;
    private static final int ATTACK_TIMEOUT = 20 * 40;

    private AtriaTools() {
    }

    // ---- спецификации инструментов для API ----

    public static JsonArray buildToolSpecs() {
        JsonArray tools = new JsonArray();
        add(tools, "get_status", "Позиция, здоровье и инвентарь компаньона, позиция владельца.", params());
        JsonObject scan = prop(prop(params(), "radius", "радиус 4..16 (по умолчанию 12)"),
                "blockFilter", "необязательный фильтр имени блока, напр. 'oak' или 'iron_ore'");
        add(tools, "scan_area",
                "Осмотреться: какие блоки есть вокруг (радиус до 16) и какие враждебные мобы рядом. "
                        + "Возвращает количества и ближайшие координаты.",
                scan);
        JsonObject walk = prop(prop(prop(params(), "x", "целое"), "y", "целое"), "z", "целое");
        add(tools, "walk_to", "Пойти к блоку с координатами (центр блока). Ждать прибытия.", walk);
        JsonObject follow = prop(params(), "on", "true или false");
        add(tools, "follow_player", "Включить/выключить следование за владельцем, когда нет задач.", follow);
        JsonObject mine = prop(prop(prop(params(), "x", "целое"), "y", "целое"), "z", "целое");
        add(tools, "mine_block",
                "Подойти и добыть блок по координатам. Дроп попадает в инвентарь компаньона.",
                mine);
        JsonObject place = prop(prop(prop(prop(params(), "x", "целое"), "y", "целое"), "z", "целое"),
                "blockFilter", "необязательный фильтр имени предмета-блока, напр. 'cobblestone'");
        add(tools, "place_block",
                "Поставить блок из своего инвентаря в точку (внутри досягаемости 4-5 блоков).",
                place);
        JsonObject attack = prop(prop(params(), "target", "hostile | any | <часть имени>"),
                "radius", "до 16 (по умолчанию 12)");
        add(tools, "attack_nearest",
                "Атаковать ближайшего моба: 'hostile' (по умолчанию), 'any' или подстрока имени, напр. 'zombie'.",
                attack);
        JsonObject give = prop(params(), "itemFilter", "необязательный фильтр имени предмета, напр. 'cobblestone'");
        add(tools, "give_to_player",
                "Передать владельцу предметы из своего инвентаря (всё или по фильтру). "
                        + "Для задач вида 'добудь и принеси' вызывай в конце.",
                give);
        JsonObject craft = prop(prop(params(), "item", "часть имени результата, напр. 'crafting_table' или 'planks'"),
                "count", "сколько раз скрафтить (по умолчанию 1)");
        add(tools, "craft_item",
                "Скрафтить предмет из своих ингредиентов. Рецепты до 4 ингредиентов доступны без верстака; "
                        + "для сложных нужен верстак в 3 блоках — если его нет, сначала построй из досок (place_block).",
                craft);
        JsonObject smelt = prop(prop(params(), "item", "часть имени того, что переплавить, напр. 'iron_ore' или 'raw_iron'"),
                "count", "сколько переплавить (по умолчанию 1, максимум 16)");
        add(tools, "smelt_item",
                "Переплавить в печке предмет из своего инвентаря. Нужна печка в 3 блоках (нет - построй из 8 булыжника) "
                        + "и топливо (уголь, доски, брёвна...): 1 топливный предмет на 1 переплавку.",
                smelt);
        JsonObject chest = prop(prop(params(), "mode", "'deposit' - сдать всё в сундук, 'withdraw' - забрать предметы из сундука"),
                "itemFilter", "необязательный фильтр имени предмета (для withdraw)");
        add(tools, "use_chest",
                "Сдать свой инвентарь в ближайший сундук (deposit) или забрать из сундука предметы (withdraw). "
                        + "Сундук должен быть в 4 блоках.",
                chest);
        add(tools, "guard_mode",
                "Режим охраны: в простое атаковать враждебных мобов в радиусе 8 блоков.",
                prop(params(), "on", "true или false"));
        add(tools, "say", "Написать короткое сообщение владельцу в чат.",
                prop(params(), "text", "текст"));
        add(tools, "finish", "Задача выполнена. Обязательно вызови в конце с кратким итогом.",
                prop(params(), "summary", "что сделано"));
        return tools;
    }

    /** Пустая JSON-схема параметров инструмента. */
    private static JsonObject params() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        schema.add("properties", new JsonObject());
        schema.add("required", new JsonArray());
        return schema;
    }

    /** Схема с одним строковым/числовым свойством для удобства объявления. */
    private static JsonObject prop(JsonObject schema, String name, String description) {
        JsonObject properties = schema.getAsJsonObject("properties");
        JsonObject p = new JsonObject();
        p.addProperty("description", description);
        properties.add(name, p);
        return schema;
    }

    private static void add(JsonArray tools, String name, String description, JsonObject parameters) {
        JsonObject function = new JsonObject();
        function.addProperty("name", name);
        function.addProperty("description", description);
        function.add("parameters", parameters);
        JsonObject tool = new JsonObject();
        tool.addProperty("type", "function");
        tool.add("function", function);
        tools.add(tool);
    }

    // ---- выполнение ----

    /**
     * Выполнить инструмент. Вызывать ТОЛЬКО с серверного потока.
     * Возвращает future со строкой-результатом для модели.
     */
    public static CompletableFuture<String> execute(AtriaCompanionEntity companion, String name, JsonObject args) {
        switch (name == null ? "" : name) {
            case "get_status" -> {
                return CompletableFuture.completedFuture(getStatus(companion));
            }
            case "scan_area" -> {
                return CompletableFuture.completedFuture(scanArea(companion, argInt(args, "radius", 12),
                        argStr(args, "blockFilter", null)));
            }
            case "walk_to" -> {
                BlockPos pos = argPos(args);
                if (pos == null) {
                    return CompletableFuture.completedFuture("ОШИБКА: нужны координаты x,y,z");
                }
                String err = radiusCheck(companion, pos);
                if (err != null) {
                    return CompletableFuture.completedFuture(err);
                }
                return companion.beginWalk(pos, WALK_TIMEOUT);
            }
            case "follow_player" -> {
                companion.setFollowOwner(argBool(args, "on", true));
                return CompletableFuture.completedFuture("следование за владельцем: "
                        + (argBool(args, "on", true) ? "включено" : "выключено"));
            }
            case "mine_block" -> {
                BlockPos pos = argPos(args);
                if (pos == null) {
                    return CompletableFuture.completedFuture("ОШИБКА: нужны координаты x,y,z");
                }
                AtriaConfig cfg = AtriaConfig.get();
                if (!cfg.agentCanBreakBlocks) {
                    return CompletableFuture.completedFuture("ОШИБКА: добыча блоков запрещена настройкой agentCanBreakBlocks");
                }
                String err = radiusCheck(companion, pos);
                if (err != null) {
                    return CompletableFuture.completedFuture(err);
                }
                Level level = companion.level;
                BlockState state = level.getBlockState(pos);
                if (state.isAir()) {
                    return CompletableFuture.completedFuture("там уже воздух: " + posStr(pos));
                }
                return companion.beginMine(pos, MINE_TIMEOUT)
                        .thenApply(result -> result + "; инвентарь: " + companion.inventorySummary());
            }
            case "place_block" -> {
                BlockPos pos = argPos(args);
                if (pos == null) {
                    return CompletableFuture.completedFuture("ОШИБКА: нужны координаты x,y,z");
                }
                AtriaConfig cfg = AtriaConfig.get();
                if (!cfg.agentCanPlaceBlocks) {
                    return CompletableFuture.completedFuture("ОШИБКА: установка блоков запрещена настройкой agentCanPlaceBlocks");
                }
                if (!companion.level.getBlockState(pos).isAir()) {
                    return CompletableFuture.completedFuture("ОШИБКА: точка " + posStr(pos) + " не пуста");
                }
                double dist = Math.sqrt(companion.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
                if (dist > 5.0) {
                    return CompletableFuture.completedFuture("ОШИБКА: точка " + posStr(pos)
                            + " слишком далеко (" + String.format("%.1f", dist) + " блоков), сначала walk_to рядом");
                }
                ItemStack blockStack = companion.takeBlockItem(argStr(args, "blockFilter", null));
                if (blockStack.isEmpty()) {
                    return CompletableFuture.completedFuture("ОШИБКА: в инвентаре нет блока"
                            + (args.has("blockFilter") ? " с фильтром '" + args.get("blockFilter").getAsString() + "'" : "")
                            + "; инвентарь: " + companion.inventorySummary());
                }
                if (!(blockStack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) {
                    return CompletableFuture.completedFuture("ОШИБКА: предмет не является блоком");
                }
                companion.level.setBlock(pos, blockItem.getBlock().defaultBlockState(), 3);
                return CompletableFuture.completedFuture("поставил блок в " + posStr(pos));
            }
            case "attack_nearest" -> {
                int radius = Math.max(2, Math.min(16, argInt(args, "radius", 12)));
                var target = companion.findAttackTarget(radius, argStr(args, "target", "hostile"));
                if (target == null) {
                    return CompletableFuture.completedFuture("подходящих целей в радиусе " + radius + " не найдено");
                }
                return companion.beginAttack(target, ATTACK_TIMEOUT);
            }
            case "give_to_player" -> {
                String result = companion.giveAllToOwner(argStr(args, "itemFilter", null));
                companion.say(result);
                return CompletableFuture.completedFuture(result);
            }
            case "craft_item" -> {
                return CompletableFuture.completedFuture(craftItem(companion,
                        argStr(args, "item", ""),
                        Math.max(1, Math.min(16, argInt(args, "count", 1)))));
            }
            case "smelt_item" -> {
                return CompletableFuture.completedFuture(smeltItem(companion,
                        argStr(args, "item", ""),
                        Math.max(1, Math.min(16, argInt(args, "count", 1)))));
            }
            case "use_chest" -> {
                String mode = argStr(args, "mode", "deposit");
                return CompletableFuture.completedFuture(useChest(companion, mode,
                        argStr(args, "itemFilter", null)));
            }
            case "guard_mode" -> {
                boolean on = argBool(args, "on", true);
                companion.setGuardMode(on);
                return CompletableFuture.completedFuture("охрана: " + (on ? "включена" : "выключена"));
            }
            case "say" -> {
                String text = argStr(args, "text", "");
                if (!text.isBlank()) {
                    companion.say(text);
                }
                return CompletableFuture.completedFuture("сообщение отправлено");
            }
            case "finish" -> {
                return CompletableFuture.completedFuture("FINISH: " + argStr(args, "summary", "задача выполнена"));
            }
            default -> {
                return CompletableFuture.completedFuture("ОШИБКА: неизвестный инструмент '" + name + "'");
            }
        }
    }

    /**
     * Крафт по реальным рецептам сервера: ищем рецепт по части имени результата,
     * проверяем/списываем ингредиенты из инвентаря компаньона, выдаём результат.
     * Рецепты с 4 и менее непустыми ингредиентами доступны без верстака (сетка 2x2),
     * для остальных нужен верстак в 3 блоках.
     */
    private static String craftItem(AtriaCompanionEntity c, String itemFilter, int count) {
        if (itemFilter == null || itemFilter.isBlank()) {
            return "ОШИБКА: укажи 'item' - часть имени результата (напр. 'crafting_table')";
        }
        Level level = c.level;
        boolean hasTable = false;
        for (BlockPos p : BlockPos.betweenClosed(
                c.blockPosition().offset(-3, -2, -3), c.blockPosition().offset(3, 2, 3))) {
            if (level.getBlockState(p).is(Blocks.CRAFTING_TABLE)) {
                hasTable = true;
                break;
            }
        }
        CraftingRecipe chosen = null;
        for (CraftingRecipe r : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            try {
                ItemStack out = r.getResultItem();
                if (out.isEmpty()) {
                    continue;
                }
                String id = Registry.ITEM.getKey(out.getItem()).getPath();
                if (!id.contains(itemFilter.toLowerCase().strip())) {
                    continue;
                }
                long need = r.getIngredients().stream().filter(i -> !i.isEmpty()).count();
                if (!hasTable && need > 4) {
                    continue;
                }
                chosen = r;
                break;
            } catch (Exception ignored) {
            }
        }
        if (chosen == null) {
            return "рецепт не найден" + (hasTable ? "" :
                    " (верстака рядом нет - доступны только рецепты до 4 ингредиентов; построй верстак из досок)");
        }
        int crafted = 0;
        for (int attempt = 0; attempt < count; attempt++) {
            java.util.List<ItemStack> toConsume = new java.util.ArrayList<>();
            boolean ok = true;
            for (Ingredient ing : chosen.getIngredients()) {
                if (ing.isEmpty()) {
                    continue;
                }
                ItemStack slot = c.findMatching(ing::test);
                if (slot.isEmpty()) {
                    ok = false;
                    break;
                }
                toConsume.add(slot);
            }
            if (!ok) {
                break;
            }
            for (ItemStack slot : toConsume) {
                c.consumeOne(slot);
            }
            ItemStack result = chosen.getResultItem().copy();
            if (!c.addStack(result) && !result.isEmpty()) {
                c.spawnAtLocation(result);
            }
            crafted++;
        }
        if (crafted == 0) {
            return "не хватило ингредиентов для '" + itemFilter + "'; инвентарь: " + c.inventorySummary();
        }
        return "скрафтил " + crafted + " x "
                + Registry.ITEM.getKey(chosen.getResultItem().getItem()).getPath()
                + "; инвентарь: " + c.inventorySummary();
    }

    /**
     * Переплавка: печка в 3 блоках, рецепт SMELTING по фильтру (по результату
     * либо по входу), 1 вход + 1 любой топливный предмет на операцию.
     */
    private static String smeltItem(AtriaCompanionEntity c, String itemFilter, int count) {
        if (itemFilter == null || itemFilter.isBlank()) {
            return "ОШИБКА: укажи 'item' - часть имени того, что переплавить";
        }
        Level level = c.level;
        boolean hasFurnace = false;
        for (BlockPos p : BlockPos.betweenClosed(
                c.blockPosition().offset(-3, -2, -3), c.blockPosition().offset(3, 2, 3))) {
            if (level.getBlockState(p).is(Blocks.FURNACE)) {
                hasFurnace = true;
                break;
            }
        }
        if (!hasFurnace) {
            return "печки в 3 блоках нет - построй её из 8 булыжника (craft_item 'furnace' + place_block)";
        }
        var recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.SMELTING);
        Recipe<Container> chosen = null;
        for (Recipe<Container> r : recipes) {
            try {
                ItemStack out = r.getResultItem();
                if (out.isEmpty()) {
                    continue;
                }
                String outId = Registry.ITEM.getKey(out.getItem()).getPath();
                if (outId.contains(itemFilter.toLowerCase().strip())) {
                    chosen = r;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        if (chosen == null) {
            // пробуем по имени входа из инвентаря (напр. 'raw_iron')
            outer:
            for (ItemStack slot : c.getInventoryView()) {
                if (slot.isEmpty()) {
                    continue;
                }
                for (Recipe<Container> r : recipes) {
                    try {
                        if (!r.getIngredients().isEmpty()
                                && r.getIngredients().get(0).test(slot)
                                && Registry.ITEM.getKey(r.getResultItem().getItem()).getPath()
                                        .contains(itemFilter.toLowerCase().strip())) {
                            chosen = r;
                            break outer;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        if (chosen == null) {
            return "рецепт переплавки для '" + itemFilter + "' не найден";
        }
        int done = 0;
        for (int attempt = 0; attempt < count; attempt++) {
            ItemStack inputSlot = ItemStack.EMPTY;
            for (ItemStack slot : c.getInventoryView()) {
                if (!slot.isEmpty() && chosen.getIngredients().get(0).test(slot)) {
                    inputSlot = slot;
                    break;
                }
            }
            if (inputSlot.isEmpty()) {
                break;
            }
            ItemStack fuelSlot = c.findMatching(AbstractFurnaceBlockEntity::isFuel);
            if (fuelSlot.isEmpty()) {
                break;
            }
            c.consumeOne(inputSlot);
            c.consumeOne(fuelSlot);
            ItemStack result = chosen.getResultItem().copy();
            if (!c.addStack(result) && !result.isEmpty()) {
                c.spawnAtLocation(result);
            }
            done++;
        }
        if (done == 0) {
            return "не хватило входа или топлива; инвентарь: " + c.inventorySummary();
        }
        return "переплавил " + done + " x " + Registry.ITEM.getKey(chosen.getResultItem().getItem()).getPath()
                + "; инвентарь: " + c.inventorySummary();
    }

    /** Сдать всё в сундук или забрать из сундука. Ближайший сундук в 4 блоках. */
    private static String useChest(AtriaCompanionEntity c, String mode, String filter) {
        Level level = c.level;
        BlockPos chestPos = null;
        for (BlockPos p : BlockPos.betweenClosed(
                c.blockPosition().offset(-4, -2, -4), c.blockPosition().offset(4, 2, 4))) {
            if (level.getBlockState(p).is(Blocks.CHEST)) {
                chestPos = p.immutable();
                break;
            }
        }
        if (chestPos == null) {
            return "сундука в 4 блоках нет";
        }
        BlockEntity be = level.getBlockEntity(chestPos);
        if (!(be instanceof Container container)) {
            return "это не сундук-контейнер";
        }
        String f = filter == null ? "" : filter.toLowerCase().strip();
        int moved;
        if ("withdraw".equals(mode)) {
            moved = 0;
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack slot = container.getItem(i);
                if (slot.isEmpty()) {
                    continue;
                }
                if (!f.isEmpty() && !Registry.ITEM.getKey(slot.getItem()).getPath().contains(f)) {
                    continue;
                }
                if (c.addStack(slot)) { // мутирует стек: остаток остаётся в сундуке
                    container.setItem(i, ItemStack.EMPTY);
                    moved++;
                }
            }
            container.setChanged();
            return "забрал из сундука " + moved + " видов предметов; инвентарь: " + c.inventorySummary();
        }
        // deposit
        moved = 0;
        for (int i = 0; i < c.getInventoryView().size(); i++) {
            ItemStack slot = c.getInventoryView().get(i);
            if (slot.isEmpty()) {
                continue;
            }
            int deposited = depositStack(container, slot);
            if (deposited > 0) {
                moved++;
                if (slot.getCount() == 0) {
                    c.clearSlot(i);
                }
            }
        }
        container.setChanged();
        return "сдал в сундук предметов из " + moved + " слотов; инвентарь: " + c.inventorySummary();
    }

    /** Вложить стек в контейнер (мутирует stack), возвращает сколько положено. */
    private static int depositStack(Container container, ItemStack stack) {
        int before = stack.getCount();
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            ItemStack slot = container.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameTags(slot, stack)) {
                int space = Math.min(slot.getMaxStackSize(), 64) - slot.getCount();
                if (space > 0) {
                    int move = Math.min(space, stack.getCount());
                    slot.grow(move);
                    stack.shrink(move);
                }
            }
        }
        for (int i = 0; i < container.getContainerSize() && !stack.isEmpty(); i++) {
            if (container.getItem(i).isEmpty()) {
                int move = Math.min(Math.min(stack.getMaxStackSize(), 64), stack.getCount());
                container.setItem(i, stack.split(move));
            }
        }
        return before - stack.getCount();
    }

    // ---- вспомогательное ----

    private static String getStatus(AtriaCompanionEntity c) {
        ServerPlayer owner = c.getOwnerPlayer();
        return "поз=" + c.blockPosition().toShortString()
                + "; здоровье=" + String.format("%.0f", c.getHealth()) + "/40"
                + (owner != null ? "; владелец=" + owner.getGameProfile().getName()
                        + " на " + owner.blockPosition().toShortString() : "; владелец офлайн")
                + "; инвентарь: " + c.inventorySummary();
    }

    private static String scanArea(AtriaCompanionEntity c, int radius, String filter) {
        radius = Math.max(4, Math.min(16, radius));
        StringBuilder sb = new StringBuilder();
        List<BlockPos> blocks = c.findBlocks(radius, filter);
        sb.append("блоки в радиусе ").append(radius).append(": ");
        if (blocks.isEmpty()) {
            sb.append("ничего не найдено");
        } else {
            // сводка по типам + до 6 ближайших позиций первого типа
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            java.util.Map<String, List<BlockPos>> samples = new java.util.LinkedHashMap<>();
            for (BlockPos p : blocks) {
                String id = net.minecraft.core.Registry.BLOCK.getKey(c.level.getBlockState(p).getBlock()).toString();
                counts.merge(id, 1, Integer::sum);
                samples.computeIfAbsent(id, k -> new java.util.ArrayList<>());
                if (samples.get(id).size() < 5) {
                    samples.get(id).add(p);
                }
            }
            sb.append(counts.size()).append(" типов; ");
            counts.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(10)
                    .forEach(e -> sb.append(e.getKey()).append(" x").append(e.getValue()).append(" "));
            String first = counts.keySet().iterator().next();
            sb.append("| ближайшие позиции '").append(first).append("': ");
            for (BlockPos p : samples.get(first)) {
                sb.append(posStr(p)).append(" ");
            }
            if (counts.size() > 10) {
                sb.append("... (и ещё ").append(counts.size() - 10).append(" типов)");
            }
        }
        List<net.minecraft.world.entity.monster.Monster> hostiles = c.findHostiles(radius);
        sb.append("; враждебные мобы: ");
        if (hostiles.isEmpty()) {
            sb.append("нет");
        } else {
            for (net.minecraft.world.entity.monster.Monster m : hostiles.stream().limit(8).toList()) {
                sb.append(net.minecraft.core.Registry.ENTITY_TYPE.getKey(m.getType()).getPath())
                        .append("@").append(posStr(m.blockPosition())).append(" ");
            }
        }
        return sb.toString();
    }

    private static String radiusCheck(AtriaCompanionEntity c, BlockPos pos) {
        AtriaConfig cfg = AtriaConfig.get();
        ServerPlayer owner = c.getOwnerPlayer();
        double maxRadius = cfg.agentActionRadius;
        double distFromOwner = owner != null
                ? Math.sqrt(owner.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5))
                : 0;
        if (owner != null && distFromOwner > maxRadius) {
            return "ОШИБКА: точка " + posStr(pos) + " слишком далеко от владельца ("
                    + String.format("%.0f", distFromOwner) + " > " + (int) maxRadius + ")";
        }
        return null;
    }

    private static int argInt(JsonObject args, String key, int def) {
        try {
            if (args != null && args.has(key) && args.get(key).isJsonPrimitive()) {
                return args.get(key).getAsInt();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static String argStr(JsonObject args, String key, String def) {
        try {
            if (args != null && args.has(key) && args.get(key).isJsonPrimitive()) {
                return args.get(key).getAsString();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static boolean argBool(JsonObject args, String key, boolean def) {
        try {
            if (args != null && args.has(key) && args.get(key).isJsonPrimitive()) {
                return args.get(key).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
        return def;
    }

    private static BlockPos argPos(JsonObject args) {
        try {
            if (args != null && args.has("x") && args.has("y") && args.has("z")) {
                return new BlockPos(args.get("x").getAsInt(), args.get("y").getAsInt(), args.get("z").getAsInt());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String posStr(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
