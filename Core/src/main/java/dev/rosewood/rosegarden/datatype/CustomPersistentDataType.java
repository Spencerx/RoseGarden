package dev.rosewood.rosegarden.datatype;

import dev.rosewood.rosegarden.utils.KeyHelper;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class CustomPersistentDataType {

    public static final PersistentDataType<byte[], UUID> UUID = new PersistentDataType<byte[], UUID>() {

        public Class<byte[]> getPrimitiveType() { return byte[].class; }
        public Class<UUID> getComplexType() { return UUID.class; }

        @Override
        public byte[] toPrimitive(UUID uuid, PersistentDataAdapterContext context) {
            return ByteBuffer.wrap(new byte[16])
                    .putLong(uuid.getMostSignificantBits())
                    .putLong(uuid.getLeastSignificantBits())
                    .array();
        }

        @Override
        public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            ByteBuffer byteBuffer = ByteBuffer.wrap(primitive);
            return new UUID(byteBuffer.getLong(), byteBuffer.getLong());
        }

    };

    public static final PersistentDataType<String, Character> CHARACTER = new PersistentDataType<String, Character>() {

        public Class<String> getPrimitiveType() { return String.class; }
        public Class<Character> getComplexType() { return Character.class; }

        @Override
        public String toPrimitive(Character character, PersistentDataAdapterContext context) {
            return String.valueOf(character);
        }

        @Override
        public Character fromPrimitive(String primitive, PersistentDataAdapterContext context) {
            return primitive.charAt(0);
        }

    };

    public static final PersistentDataType<String, World> WORLD = new PersistentDataType<String, World>() {

        public Class<String> getPrimitiveType() { return String.class; }
        public Class<World> getComplexType() { return World.class; }

        @Override
        public String toPrimitive(World world, PersistentDataAdapterContext context) {
            return String.valueOf(world);
        }

        @Override
        public World fromPrimitive(String primitive, PersistentDataAdapterContext context) {
            World world = Bukkit.getWorld(primitive);
            if (world == null)
                throw new IllegalArgumentException("World is not loaded: " + primitive);
            return world;
        }

    };

    public static <T extends Enum<T>> PersistentDataType<String, T> forEnum(Class<T> enumClass) {
        return new PersistentDataType<String, T>() {

            public Class<String> getPrimitiveType() { return String.class; }
            public Class<T> getComplexType() { return enumClass; }

            @Override
            public String toPrimitive(T enumValue, PersistentDataAdapterContext context) {
                return enumValue.name();
            }

            @Override
            public T fromPrimitive(String primitive, PersistentDataAdapterContext context) {
                return Enum.valueOf(enumClass, primitive);
            }

        };
    }

    public static <T extends Keyed> PersistentDataType<String, T> forKeyed(Class<T> keyedClass, Function<NamespacedKey, T> valueOfFunction) {
        return new PersistentDataType<String, T>() {

            public Class<String> getPrimitiveType() { return String.class; }
            public Class<T> getComplexType() { return keyedClass; }

            @Override
            public String toPrimitive(T enumValue, PersistentDataAdapterContext context) {
                return enumValue.getKey().asString();
            }

            @Override
            public T fromPrimitive(String primitive, PersistentDataAdapterContext context) {
                return valueOfFunction.apply(NamespacedKey.fromString(primitive));
            }

        };
    }

    @SuppressWarnings("unchecked")
    public static <T> PersistentDataType<PersistentDataContainer, T[]> forArray(PersistentDataType<?, T> arrayElementDataType) {
        return new PersistentDataType<PersistentDataContainer, T[]>() {

            public Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
            public Class<T[]> getComplexType() { return (Class<T[]>) arrayElementDataType.getComplexType().arrayType(); }

            @Override
            public PersistentDataContainer toPrimitive(T[] array, PersistentDataAdapterContext context) {
                PersistentDataContainer container = context.newPersistentDataContainer();
                container.set(KeyHelper.get("size"), PersistentDataType.INTEGER, array.length);
                for (int i = 0; i < array.length; i++) {
                    T element = array[i];
                    if (element == null)
                        continue;
                    NamespacedKey elementKey = KeyHelper.get(String.valueOf(i));
                    container.set(elementKey, arrayElementDataType, element);
                }
                return container;
            }

            @Override
            public T[] fromPrimitive(PersistentDataContainer container, PersistentDataAdapterContext context) {
                Integer size = container.get(KeyHelper.get("size"), PersistentDataType.INTEGER);
                if (size == null)
                    throw new IllegalArgumentException("Invalid " + arrayElementDataType.getComplexType().getSimpleName() + "[]");
                T[] array = (T[]) Array.newInstance(arrayElementDataType.getComplexType(), size);
                for (int i = 0; i < size; i++) {
                    NamespacedKey elementKey = KeyHelper.get(String.valueOf(i));
                    array[i] = container.get(elementKey, arrayElementDataType);
                }
                return array;
            }

        };
    }

    @SuppressWarnings("unchecked")
    public static <T> PersistentDataType<PersistentDataContainer, List<T>> forList(PersistentDataType<?, T> listElementDataType) {
        return new PersistentDataType<PersistentDataContainer, List<T>>() {

            public Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
            public Class<List<T>> getComplexType() { return (Class<List<T>>) (Class<?>) List.class; }

            @Override
            public PersistentDataContainer toPrimitive(List<T> list, PersistentDataAdapterContext context) {
                PersistentDataContainer container = context.newPersistentDataContainer();
                container.set(KeyHelper.get("size"), PersistentDataType.INTEGER, list.size());
                for (int i = 0; i < list.size(); i++) {
                    T element = list.get(i);
                    if (element == null)
                        continue;
                    NamespacedKey elementKey = KeyHelper.get(String.valueOf(i));
                    container.set(elementKey, listElementDataType, element);
                }
                return container;
            }

            @Override
            public List<T> fromPrimitive(PersistentDataContainer container, PersistentDataAdapterContext context) {
                Integer size = container.get(KeyHelper.get("size"), PersistentDataType.INTEGER);
                if (size == null)
                    throw new IllegalArgumentException("Invalid List<" + listElementDataType.getComplexType().getSimpleName() + ">");
                List<T> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    NamespacedKey elementKey = KeyHelper.get(String.valueOf(i));
                    list.add(container.get(elementKey, listElementDataType));
                }
                return list;
            }

        };
    }

    @SuppressWarnings("unchecked")
    public static <K, V> PersistentDataType<PersistentDataContainer, Map<K, V>> forMap(PersistentDataType<?, K> keyDataType, PersistentDataType<?, V> valueDataType) {
        return new PersistentDataType<PersistentDataContainer, Map<K, V>>() {

            public Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
            public Class<Map<K, V>> getComplexType() { return (Class<Map<K, V>>) (Class<?>) Map.class; }

            @Override
            public PersistentDataContainer toPrimitive(Map<K, V> map, PersistentDataAdapterContext context) {
                PersistentDataContainer container = context.newPersistentDataContainer();
                container.set(KeyHelper.get("size"), PersistentDataType.INTEGER, map.size());
                int index = 0;
                for (Map.Entry<K, V> entry : map.entrySet()) {
                    K keyValue = entry.getKey();
                    V valueValue = entry.getValue();
                    if (valueValue == null)
                        continue;
                    NamespacedKey keyKey = this.makeKey(true, index);
                    NamespacedKey valueKey = this.makeKey(false, index);
                    container.set(keyKey, keyDataType, keyValue);
                    container.set(valueKey, valueDataType, valueValue);
                    index++;
                }
                return container;
            }

            @Override
            public Map<K, V> fromPrimitive(PersistentDataContainer container, PersistentDataAdapterContext context) {
                Integer size = container.get(KeyHelper.get("size"), PersistentDataType.INTEGER);
                if (size == null)
                    throw new IllegalArgumentException("Invalid Map<" + keyDataType.getComplexType().getSimpleName() + ", " + valueDataType.getComplexType().getSimpleName() + ">");
                Map<K, V> map = new HashMap<>();
                for (int i = 0; i < size; i++) {
                    NamespacedKey keyKey = this.makeKey(true, i);
                    NamespacedKey valueKey = this.makeKey(false, i);
                    K key = container.get(keyKey, keyDataType);
                    V value = container.get(valueKey, valueDataType);
                    map.put(key, value);
                }
                return map;
            }

            private NamespacedKey makeKey(boolean key, int index) {
                return KeyHelper.get(String.format("%s-%d", key ? "key" : "value", index));
            }

        };
    }

}
