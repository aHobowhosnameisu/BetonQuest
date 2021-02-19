package pl.betoncraft.betonquest.compatibility.mmogroup.mmoitems;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.betoncraft.betonquest.Instruction;
import pl.betoncraft.betonquest.VariableNumber;
import pl.betoncraft.betonquest.api.QuestEvent;
import pl.betoncraft.betonquest.config.Config;
import pl.betoncraft.betonquest.exceptions.InstructionParseException;
import pl.betoncraft.betonquest.exceptions.QuestRuntimeException;
import pl.betoncraft.betonquest.utils.LogUtils;
import pl.betoncraft.betonquest.utils.PlayerConverter;

import java.util.logging.Level;

@SuppressWarnings("PMD.CommentRequired")
public class MMOItemsTakeEvent extends QuestEvent {

    private final Type itemType;
    private final String itemID;
    private final boolean notify;
    private VariableNumber deleteAmountVar = new VariableNumber(1);

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public MMOItemsTakeEvent(final Instruction instruction) throws InstructionParseException {
        super(instruction, true);

        itemType = MMOItems.plugin.getTypes().get(instruction.next());
        itemID = instruction.next();
        if (instruction.size() > 3) {
            deleteAmountVar = instruction.getVarNum();
        }
        notify = instruction.hasArgument("notify");
    }

    @SuppressWarnings({"PMD.PreserveStackTrace", "PMD.CyclomaticComplexity"})
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    @Override
    protected Void execute(final String playerID) throws QuestRuntimeException {
        final Inventory inv = PlayerConverter.getPlayer(playerID).getInventory();
        final ItemStack[] inventoryItems = inv.getContents();
        String itemName = itemID;
        final int deleteAmount = deleteAmountVar.getInt(playerID);

        int deletedCounter = 0;

        for (final ItemStack item : inventoryItems) {
            if (item == null) {
                continue;
            }
            final NBTItem realItemNBT = NBTItem.get(item);
            final String realItemType = realItemNBT.getString("MMOITEMS_ITEM_TYPE");
            final String realItemID = realItemNBT.getString("MMOITEMS_ITEM_ID");

            if (realItemID.equalsIgnoreCase(itemID) && realItemType.equalsIgnoreCase(itemType.getId())) {
                itemName = item.getItemMeta().getDisplayName();
                for (int i = item.getAmount(); i > 0; i--) {
                    if (deletedCounter < deleteAmount) {
                        item.setAmount(item.getAmount() - 1);
                        deletedCounter++;
                    }
                }
            }
        }
        inv.setContents(inventoryItems);

        if (notify && deletedCounter > 1) {
            try {
                Config.sendNotify(instruction.getPackage().getName(), playerID, "items_taken",
                        new String[]{itemName, String.valueOf(deletedCounter)},
                        "items_taken,info");
            } catch (final QuestRuntimeException exception) {
                LogUtils.getLogger().log(Level.WARNING, "The notify system was unable to play a sound for the 'items_taken' category in '" + getFullId() + "'. Error was: '" + exception.getMessage() + "'");
                LogUtils.logThrowable(exception);
            }
        }
        return null;
    }
}

