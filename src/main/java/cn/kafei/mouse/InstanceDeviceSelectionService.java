package cn.kafei.mouse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InstanceDeviceSelectionService {
    private static final Set<Integer> SELECTED_DEVICE_IDS = ConcurrentHashMap.newKeySet();

    private InstanceDeviceSelectionService() {
    }

    // 返回当前实例内存中的已选设备 ID 列表。
    public static List<Integer> getSelectedDeviceIds() {
        return new ArrayList<>(SELECTED_DEVICE_IDS);
    }

    // 用新的选择结果原子替换当前实例的设备集合。
    public static synchronized void setSelectedDeviceIds(Collection<Integer> deviceIds) {
        SELECTED_DEVICE_IDS.clear();
        SELECTED_DEVICE_IDS.addAll(deviceIds);
    }
}
