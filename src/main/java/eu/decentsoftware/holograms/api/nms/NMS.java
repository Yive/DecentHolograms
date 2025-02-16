package eu.decentsoftware.holograms.api.nms;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import eu.decentsoftware.holograms.api.utils.reflect.ReflectConstructor;
import eu.decentsoftware.holograms.api.utils.reflect.ReflectField;
import eu.decentsoftware.holograms.api.utils.reflect.ReflectionUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class NMS {

    @Getter
    private static NMS instance;

    public static void init() {
        instance = new NMS();
    }

    protected static final Map<EntityType, Float> entityDimensions;

    static {
        ImmutableMap.Builder<EntityType, Float> builder = ImmutableMap.builder();
        for (net.minecraft.world.entity.EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            builder.put(CraftEntityType.minecraftToBukkit(type), type.getHeight());
        }
        entityDimensions = builder.build();
    }

    // PACKETS
    private static final ReflectConstructor PACKET_MOUNT_CONSTRUCTOR;
    // DATA WATCHER OBJECT
    private static final EntityDataAccessor<Optional<Component>> DWO_CUSTOM_NAME;
    private static final EntityDataAccessor<Boolean> DWO_CUSTOM_NAME_VISIBLE;
    private static final EntityDataAccessor<Byte> DWO_ENTITY_DATA;
    private static final EntityDataAccessor<net.minecraft.world.item.ItemStack> DWO_ITEM;

    static {
        // PACKETS - Why did you have to make this private...
        PACKET_MOUNT_CONSTRUCTOR = new ReflectConstructor(
                ReflectionUtil.getNMClass("network.protocol.game.ClientboundSetPassengersPacket"), FriendlyByteBuf.class
        );

        // DATA WATCHER OBJECT - Stupid private/protected fields...
        DWO_ENTITY_DATA = ((EntityDataAccessor<Byte>) new ReflectField<>(Entity.class, "DATA_SHARED_FLAGS_ID").getValue(null));
        DWO_ITEM = ((EntityDataAccessor<net.minecraft.world.item.ItemStack>) new ReflectField<>(ItemEntity.class, "DATA_ITEM").getValue(null));
        DWO_CUSTOM_NAME = ((EntityDataAccessor<Optional<Component>>) new ReflectField<>(Entity.class, "DATA_CUSTOM_NAME").getValue(null));
        DWO_CUSTOM_NAME_VISIBLE = ((EntityDataAccessor<Boolean>) new ReflectField<>(Entity.class, "DATA_CUSTOM_NAME_VISIBLE").getValue(null));
    }

    public int getFreeEntityId() {
        return Bukkit.getUnsafe().nextEntityId();
    }

    public float getEntityHeight(EntityType type) {
        return entityDimensions.getOrDefault(type, 0.0f);
    }

    public void showFakeEntity(Player player, Location location, EntityType entityType, int entityId) {
        Validate.notNull(player);
        Validate.notNull(location);
        Validate.notNull(entityType);

        sendPacket(player, new ClientboundAddEntityPacket(
                entityId,
                UUID.randomUUID(),
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                CraftEntityType.bukkitToMinecraft(entityType),
                0,
                Vec3.ZERO,
                location.getYaw()
        ));
        teleportFakeEntity(player, location, entityId);
    }

    public void showFakeEntityLiving(Player player, Location location, EntityType entityType, int entityId) {
        Validate.notNull(player);
        Validate.notNull(location);
        Validate.notNull(entityType);

        showFakeEntity(player, location, entityType, entityId);
    }

    private void sendEntityMetadata(Player player, int entityId, List<SynchedEntityData.DataItem<?>> items) {
        Validate.notNull(player);
        Validate.notNull(items);

        List<SynchedEntityData.DataValue<?>> values = new ArrayList<>();
        for (SynchedEntityData.DataItem<?> item : items) {
            values.add(item.value());
        }

        sendPacket(player, new ClientboundSetEntityDataPacket(entityId, values));
    }

    public void showFakeEntityArmorStand(Player player, Location location, int entityId, boolean invisible, boolean small, boolean clickable) {
        Validate.notNull(player);
        Validate.notNull(location);

        List<SynchedEntityData.DataItem<?>> dataWatcherItems = new ArrayList<>();

        SynchedEntityData.DataItem<Byte> sharedDataFlags = new SynchedEntityData.DataItem<>(DWO_ENTITY_DATA, (byte) 0);
        if (invisible) {
            sharedDataFlags.setValue((byte)(sharedDataFlags.getValue() | 1 << Entity.FLAG_INVISIBLE));
        } else {
            sharedDataFlags.setValue((byte)(sharedDataFlags.getValue() & ~(1 << Entity.FLAG_INVISIBLE)));
        }
        dataWatcherItems.add(sharedDataFlags);

        SynchedEntityData.DataItem<Byte> clientFlags = new SynchedEntityData.DataItem<>(ArmorStand.DATA_CLIENT_FLAGS, (byte) 0);
        clientFlags.setValue(this.setBit(clientFlags.getValue(), ArmorStand.CLIENT_FLAG_SMALL, small));
        clientFlags.setValue(this.setBit(clientFlags.getValue(), ArmorStand.CLIENT_FLAG_MARKER, clickable)); // Marker?
        dataWatcherItems.add(clientFlags);

        showFakeEntityLiving(player, location, EntityType.ARMOR_STAND, entityId);
        sendEntityMetadata(player, entityId, dataWatcherItems);
    }

    public void showFakeEntityItem(Player player, Location location, ItemStack itemStack, int entityId) {
        Validate.notNull(player);
        Validate.notNull(location);
        Validate.notNull(itemStack);

        List<SynchedEntityData.DataItem<?>> dataWatcherItems = new ArrayList<>();
        dataWatcherItems.add(new SynchedEntityData.DataItem<>(DWO_ITEM, CraftItemStack.asNMSCopy(itemStack)));
        showFakeEntity(player, location, EntityType.ITEM, entityId);
        sendEntityMetadata(player, entityId, dataWatcherItems);
        teleportFakeEntity(player, location, entityId);
    }

    public void updateFakeEntityCustomName(Player player, String name, int entityId) {
        Validate.notNull(player);
        Validate.notNull(name);

        List<SynchedEntityData.DataItem<?>> dataWatcherItems = new ArrayList<>();
        dataWatcherItems.add(new SynchedEntityData.DataItem<>(DWO_CUSTOM_NAME, Optional.ofNullable(CraftChatMessage.fromStringOrNull(name))));
        dataWatcherItems.add(new SynchedEntityData.DataItem<>(DWO_CUSTOM_NAME_VISIBLE, !ChatColor.stripColor(name).isEmpty()));
        sendEntityMetadata(player, entityId, dataWatcherItems);
    }

    public void updateFakeEntityItem(Player player, ItemStack itemStack, int entityId) {
        Validate.notNull(player);
        Validate.notNull(itemStack);

        List<SynchedEntityData.DataItem<?>> dataWatcherItems = new ArrayList<>();
        dataWatcherItems.add(new SynchedEntityData.DataItem<>(DWO_ITEM, CraftItemStack.asNMSCopy(itemStack)));
        sendEntityMetadata(player, entityId, dataWatcherItems);
    }

    public void teleportFakeEntity(Player player, Location location, int entityId) {
        Validate.notNull(player);
        Validate.notNull(location);

        sendPacket(player, new ClientboundTeleportEntityPacket(
                entityId,
                new PositionMoveRotation(
                        new Vec3(location.getX(), location.getY(), location.getZ()),
                        Vec3.ZERO,
                        location.getYaw(),
                        location.getPitch()
                ),
                new HashSet<>(),
                false
        ));
    }

    public void helmetFakeEntity(Player player, ItemStack itemStack, int entityId) {
        Validate.notNull(player);
        Validate.notNull(itemStack);

        List<Pair<EquipmentSlot, net.minecraft.world.item.ItemStack>> items = new ArrayList<>();
        items.add(Pair.of(EquipmentSlot.HEAD, CraftItemStack.asNMSCopy(itemStack)));
        sendPacket(player, new ClientboundSetEquipmentPacket(entityId, items));
    }

    public void attachFakeEntity(Player player, int vehicleId, int entityId) {
        Validate.notNull(player);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(vehicleId);
        buf.writeVarIntArray(new int[]{entityId});
        sendPacket(player, PACKET_MOUNT_CONSTRUCTOR.newInstance(buf));
    }

    public void hideFakeEntities(Player player, int... entityIds) {
        Validate.notNull(player);
        sendPacket(player, new ClientboundRemoveEntitiesPacket(entityIds));
    }

    public ChannelPipeline getPipeline(Player player) {
        return ((CraftPlayer) player).getHandle().connection.connection.channel.pipeline();
    }

    private void sendPacket(Player player, Packet<?> packet) {
        ((CraftPlayer) player).getHandle().connection.sendPacket(packet);
    }

    private byte setBit(byte b, int offset, boolean bool) {
        return (byte) (bool ? b | offset : b & ~offset);
    }
}
