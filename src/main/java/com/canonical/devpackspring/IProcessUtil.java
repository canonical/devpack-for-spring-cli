package com.canonical.devpackspring;

import org.springframework.cli.util.TerminalMessage;

import java.io.IOException;

public interface IProcessUtil {
    int runProcess(final TerminalMessage message, boolean inheritIO, String ...args) throws IOException;
}
