package com.xara.gamespace.shizuku;

interface IXaraShellService {
    String runCommand(String command);
    int getServerUid();
    void destroy();
}
