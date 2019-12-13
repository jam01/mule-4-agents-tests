package com.jam01.mule.tests;


import org.jboss.byteman.agent.Main;

import java.lang.instrument.Instrumentation;

public class BytemanAgent {

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        System.out.println("from byteman: agent premain");
        Main.premain(agentArgs, inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        Main.agentmain(agentArgs, inst);
    }
}
