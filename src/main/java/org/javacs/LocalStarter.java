package org.javacs;

import java.util.logging.Logger;
import java.util.logging.Level;
import org.javacs.lsp.LSP;

/**
 * This class is an executable reproduction of the "Main.java" for this Language-Server project<br>
 * It vows to be launched in a Thread instead of using the JLS as a whole process
 */
public class LocalStarter
{
    private static final Logger LOG = Logger.getLogger("main");

    public LocalStarter()
    {
        // Empty constructor...
    }

    public static void setRootFormat()
    {
        var root = Logger.getLogger("");

        for (var h : root.getHandlers())
        {
            h.setFormatter(new LogFormat());
        }
    }

    public void start(boolean quiet)
    {
        if (quiet)
            LOG.setLevel(Level.OFF);

            try
            {
                setRootFormat();
                LSP.connect(JavaLanguageServer::new, System.in, System.out);
            }
            catch (Throwable t)
            {
                LOG.log(Level.SEVERE, t.getMessage(), t);
                System.exit(1);
            }
    }
}
