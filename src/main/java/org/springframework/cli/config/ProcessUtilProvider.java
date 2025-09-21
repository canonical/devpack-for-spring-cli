package org.springframework.cli.config;

import com.canonical.devpackspring.IProcessUtil;
import com.canonical.devpackspring.ProcessUtil;
import org.springframework.cli.util.TerminalMessage;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ProcessUtilProvider implements IProcessUtil {
    @Override
    public int runProcess(TerminalMessage message, boolean inheritIO, String... args) throws IOException {
        return ProcessUtil.runProcess(message, inheritIO, args);
    }
}
