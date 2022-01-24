package org.sec.core.service;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.sec.core.asm.DOSClassVisitor;
import org.sec.core.asm.XXEClassVisitor;
import org.sec.core.spring.SpringController;
import org.sec.core.spring.SpringMapping;
import org.sec.log.SLF4J;
import org.sec.model.CallGraph;
import org.sec.model.ClassFile;
import org.sec.model.MethodReference;
import org.sec.model.ResultInfo;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SLF4J
public class DOSService {
    private static Logger logger;

    private static Map<MethodReference.Handle, Set<CallGraph>> allCalls;
    private static Map<String, ClassFile> classFileMap;
    private static final List<String> tempChain = new ArrayList<>();
    private static final List<ResultInfo> results = new ArrayList<>();

    public static void start(Map<String, ClassFile> classFileByName,
                             List<SpringController> controllers,
                             Map<MethodReference.Handle, Set<CallGraph>> discoveredCalls) {
        allCalls = discoveredCalls;
        classFileMap = classFileByName;

        logger.info("start analysis dos");
        for (SpringController controller : controllers) {
            for (SpringMapping mapping : controller.getMappings()) {
                MethodReference methodReference = mapping.getMethodReference();
                if (methodReference == null) {
                    continue;
                }
                Type[] argTypes = Type.getArgumentTypes(methodReference.getDesc());
                Type[] extendedArgTypes = new Type[argTypes.length + 1];
                System.arraycopy(argTypes, 0, extendedArgTypes, 1, argTypes.length);
                argTypes = extendedArgTypes;
                boolean[] vulnerableIndex = new boolean[argTypes.length];
                for (int i = 1; i < argTypes.length; i++) {
                    vulnerableIndex[i] = true;
                }
                Set<CallGraph> calls = allCalls.get(methodReference.getHandle());
                if (calls == null || calls.size() == 0) {
                    continue;
                }
                tempChain.add(methodReference.getClassReference().getName() + "." + methodReference.getName());
                for (CallGraph callGraph : calls) {
                    int callerIndex = callGraph.getCallerArgIndex();
                    if (callerIndex == -1) {
                        continue;
                    }
                    if (vulnerableIndex[callerIndex]) {
                        tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                                callGraph.getTargetMethod().getName());
                        List<MethodReference.Handle> visited = new ArrayList<>();
                        doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
                    }
                }
                tempChain.clear();
            }
        }
    }

    public static List<ResultInfo> getResults() {
        return results;
    }

    private static void doTask(MethodReference.Handle targetMethod, int targetIndex,
                               List<MethodReference.Handle> visited) {
        if (visited.contains(targetMethod)) {
            return;
        } else {
            visited.add(targetMethod);
        }
        ClassFile file = classFileMap.get(targetMethod.getClassReference().getName());
        try {
            if (file == null) {
                return;
            }
            ClassReader cr = new ClassReader(file.getFile());
            DOSClassVisitor cv = new DOSClassVisitor(targetMethod, targetIndex);
            cr.accept(cv, ClassReader.EXPAND_FRAMES);
            if (cv.getPass("RE-DOS") != null && cv.getPass("RE-DOS")) {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.MID_RISK);
                resultInfo.setVulName("RE DOS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                logger.info("detect re dos");
                System.out.println(resultInfo);
            }
            if (cv.getPass("ARRAY-DOS") != null && cv.getPass("ARRAY-DOS")) {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.MID_RISK);
                resultInfo.setVulName("ARRAY DOS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                logger.info("detect array dos");
                System.out.println(resultInfo);
            }
            if (cv.getPass("FOR-DOS") != null && cv.getPass("FOR-DOS")) {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.MID_RISK);
                resultInfo.setVulName("FOR DOS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                logger.info("detect for dos");
                System.out.println(resultInfo);
            }
            if (cv.getPass("LIST-DOS") != null && cv.getPass("LIST-DOS")) {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.MID_RISK);
                resultInfo.setVulName("LIST DOS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                logger.info("detect list dos");
                System.out.println(resultInfo);
            }
            if (cv.getPass("MAP-DOS") != null && cv.getPass("MAP-DOS")) {
                ResultInfo resultInfo = new ResultInfo();
                resultInfo.setRisk(ResultInfo.MID_RISK);
                resultInfo.setVulName("MAP DOS");
                resultInfo.getChains().addAll(tempChain);
                results.add(resultInfo);
                logger.info("detect map dos");
                System.out.println(resultInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Set<CallGraph> calls = allCalls.get(targetMethod);
        if (calls == null || calls.size() == 0) {
            return;
        }
        for (CallGraph callGraph : calls) {
            if (callGraph.getCallerArgIndex() == targetIndex && targetIndex != -1) {
                if (visited.contains(callGraph.getTargetMethod())) {
                    return;
                }
                tempChain.add(callGraph.getTargetMethod().getClassReference().getName() + "." +
                        callGraph.getTargetMethod().getName());
                doTask(callGraph.getTargetMethod(), callGraph.getTargetArgIndex(), visited);
            }
        }
    }
}
