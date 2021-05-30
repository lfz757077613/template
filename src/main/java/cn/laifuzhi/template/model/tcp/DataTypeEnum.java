package cn.laifuzhi.template.model.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum DataTypeEnum {
    PING((byte) 1, Ping.class, "heartbeat request"),
    PONG((byte) 2, Pong.class, "heartbeat response"),
    ;
    private static final Map<Byte, DataTypeEnum> typeEnumMap = new HashMap<>();
    private final byte type;
    private final Class<? extends BaseDTO> dataClass;
    private final String desc;

    static {
        for (DataTypeEnum dataTypeEnum : values()) {
            typeEnumMap.put(dataTypeEnum.getType(), dataTypeEnum);
        }
    }

    public static Optional<DataTypeEnum> getByType(byte type) {
        return Optional.ofNullable(typeEnumMap.get(type));
    }

    public static boolean contains(byte type) {
        return typeEnumMap.containsKey(type);
    }
}
