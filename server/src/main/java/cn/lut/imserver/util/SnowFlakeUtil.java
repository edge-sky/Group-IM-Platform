package cn.lut.imserver.util;

public class SnowFlakeUtil {
    private static long startTimeStamp; // 起始时间戳
    private static long workID; // 机器ID
    private static long dataCenterID; // 数据中心ID
    private static long sequence; // 序列号
    private static long dataCenterIndex; // 数据中心ID移动位数
    private static long workIDIndex; // 机器ID移动位数
    private static long timeStampIndex; // 时间戳移动位数
    private static long lastTimeStamp; // 记录上一次时间戳
    private static long sequenceMask; // 序列号掩码
    private static long sequenceLength; // 序列号长度12位

    // 初始化
    static {
        startTimeStamp = 1756902138329L;
        workID = 1L;
        dataCenterID = 1L;
        sequence = 0L; // 起始序列号 0开始
        dataCenterIndex = 12L; // 数据中心位移位数
        workIDIndex = 17L; // 机器ID位移位数
        timeStampIndex = 22L; // 时间戳位移位数
        lastTimeStamp = -1L; // 记录上次时间戳
        sequenceLength = 12L; // 序列号长度
        sequenceMask = ~(-1L << sequenceLength); // 序列号掩码
    }

    public static synchronized long getId() {
        long now = System.currentTimeMillis();
        if (now < lastTimeStamp) {
            throw new RuntimeException("时钟回拨错误，ID 生成失败");
        }

        if (now == lastTimeStamp) {
            // 时间戳相同
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0L) {
                // 溢出
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            now = System.currentTimeMillis();
        } else {
            // 归零
            sequence = 0L;
        }

        long res = 0;
        res |= sequence;
        res |= dataCenterID << dataCenterIndex;
        res |= workID << workIDIndex;
        res |= (now - startTimeStamp) << timeStampIndex;
        lastTimeStamp = now;

        return res;
    }
}
