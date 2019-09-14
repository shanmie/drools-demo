package com.example.drools.util;

import lombok.NoArgsConstructor;
import org.drools.decisiontable.InputType;
import org.drools.decisiontable.SpreadsheetCompiler;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.io.Resource;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.utils.KieHelper;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

/**
 * 1.维护了有状态和无状态的两种Session对象，不推荐使用get。
 * 2.全局的Session已经在config下的Configuration中自动装配了。
 * 3.无并发的情况下，使用newKieSession()既可。
 *
 * @author 2018/04/04
 */
@NoArgsConstructor
public class KieSessionUtil {
    private static KieSession kieSession;
    private static final String RULES_PATH = "rules/";

    /**
     * @return KieSession
     * @throws Exception
     * @description TODO(创建包含所有规则的对象)
     */
    public static KieSession getAllRules() throws Exception {
        try {
            disposeKieSession();
            KieServices kieServices = KieServices.Factory.get();
            KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
            for (org.springframework.core.io.Resource file : new PathMatchingResourcePatternResolver().getResources("classpath*:" + RULES_PATH + "**/*.*")) {
                kieFileSystem.write(org.kie.internal.io.ResourceFactory.newClassPathResource(RULES_PATH + file.getFilename(), "UTF-8"));
            }
            final KieRepository kieRepository = KieServices.Factory.get().getRepository();
            kieRepository.addKieModule(new KieModule() {
                @Override
                public ReleaseId getReleaseId() {
                    return kieRepository.getDefaultReleaseId();
                }
            });
            KieBuilder kieBuilder = KieServices.Factory.get().newKieBuilder(kieFileSystem);
            kieBuilder.buildAll();
            kieSession = KieServices.Factory.get().newKieContainer(kieRepository.getDefaultReleaseId()).newKieSession().getKieBase().newKieSession();
            return kieSession;
        } catch (Exception ex) {
            throw ex;
        }
    }

    /**
     * @param path 绝对路径
     * @return KieSession 有状态
     * @description TODO (快速新建KieSession 默认项目路径)
     */
    public static KieSession newKieSession(String path) throws Exception {
        return newKieSession(path, false);
    }

    /**
     * @param path 绝对路径
     * @return StatelessKieSession 无状态
     * @description TODO (快速新建StatelessKieSession 默认项目路径)
     */
    public static StatelessKieSession newStatelessKieSession(String path) throws Exception {
        return newStatelessKieSession(path, false);
    }

    /**
     * @param path             绝对路径
     * @param enableSystemPath 是否启用系统路径
     * @return KieSession 有状态
     */
    public static KieSession newKieSession(String path, boolean enableSystemPath) throws Exception {
        KieSession kieSession = getKieBase(path, enableSystemPath).newKieSession();
        kieSession.addEventListener(new DebugRuleRuntimeEventListener());
        return kieSession;

    }

    /**
     * @param path             绝对路径
     * @param enableSystemPath 是否启用系统路径
     * @return StatelessKieSession 无状态
     */
    public static StatelessKieSession newStatelessKieSession(String path, boolean enableSystemPath) throws Exception {
        StatelessKieSession kieSession = getKieBase(path, enableSystemPath).newStatelessKieSession();
        return kieSession;

    }

    /**
     * @return void
     * @description TODO (清空对象)
     * @title disposeKieSession 重置KieSession
     */
    public static void disposeKieSession() {
        if (kieSession != null) {
            kieSession.dispose();
            kieSession = null;
        }
    }


    /**
     * 加载外部规则文件
     *
     * @param path         文件路径
     * @param isSystemPath 是否是系统路径
     * @return
     * @throws Exception
     */
    protected static KieBase getKieBase(String path, boolean isSystemPath) throws Exception {
        KieServices kieServices = KieServices.Factory.get();
        KieFileSystem kfs = kieServices.newKieFileSystem();
        Resource resource;
        if (isSystemPath) {
            resource = ResourceFactory.newFileResource(path);
        } else {
            resource = kieServices.getResources().newClassPathResource(path);
        }
        kfs.write(resource);
        KieBuilder kieBuilder = kieServices.newKieBuilder(kfs).buildAll();
        if (kieBuilder.getResults().getMessages(Message.Level.ERROR).size() > 0) {
            throw new Exception();
        }
        KieContainer kieContainer = kieServices.newKieContainer(kieServices.getRepository().getDefaultReleaseId());
        KieBase kBase = kieContainer.getKieBase();
        return kBase;
    }

    /**
     * 根据服务器真实路径下的xls文件生成drl文件内容
     */
    public static KieSession getKieSessionFromXLS(String realPath) throws FileNotFoundException {
        return createKieSessionFromDRL(getDRL(realPath));
    }

    // 把xls文件解析为String
    public static String getDRL(String realPath) throws FileNotFoundException {
        File file = new File(realPath); // 例如：C:\\abc.xls
        InputStream is = new FileInputStream(file);
        SpreadsheetCompiler compiler = new SpreadsheetCompiler();
        return compiler.compile(is, InputType.XLS);
    }

    // drl为含有内容的字符串
    public static KieSession createKieSessionFromDRL(String drl) {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addContent(drl, ResourceType.DRL);
        Results results = kieHelper.verify();
        if (results.hasMessages(Message.Level.WARNING, Message.Level.ERROR)) {
            List<Message> messages = results.getMessages(Message.Level.WARNING, Message.Level.ERROR);
            for (Message message : messages) {
                System.out.println("Error: " + message.getText());
            }
            throw new IllegalStateException("Compilation errors were found. Check the logs.");
        }
        return kieHelper.build().newKieSession();
    }


}
