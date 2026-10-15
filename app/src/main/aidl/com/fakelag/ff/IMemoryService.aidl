package com.fakelag.ff;

interface IMemoryService {
    int findPid(String packageName);
    boolean freezeEntities(int pid, boolean enable);
    boolean forceKillAfterDamage(int pid, int damageThreshold);
}
