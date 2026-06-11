package dev.beryl.lattice.update;

public interface UpdateService {
    UpdateCheckResult check(UpdateSource source);

    UpdateInstallResult install(UpdateInstallRequest request);
}
