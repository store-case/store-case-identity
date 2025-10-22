package com.leedahun.storecaseidentity.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspectConfig {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {}

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayerByAnnotation() {}

    @Around("controllerLayer()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String sig = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("➡️ Controller enter {} args={}", sig, Arrays.toString(args));
        try {
            Object ret = joinPoint.proceed();
            log.info("⬅️ Controller exit  {} result={} ({} ms)",
                    sig, shrink(ret), System.currentTimeMillis() - start);
            return ret;
        } catch (Throwable t) {
            log.error("💥 Controller error {} msg={}", sig, t.getMessage(), t);
            throw t;
        }
    }

    @Around("serviceLayerByAnnotation()")
    public Object logService(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String sig = joinPoint.getSignature().toShortString();

        if (log.isDebugEnabled()) {
            log.debug("▶ Service enter {}", sig);
        }
        try {
            Object ret = joinPoint.proceed();
            long time = System.currentTimeMillis() - start;
            log.info("◀ Service exit  {} time={}", sig, time);
            return ret;
        } catch (Throwable t) {
            log.error("🔥 Service error {} msg={}", sig, t.getMessage(), t);
            throw t;
        }
    }

    private String shrink(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        return s.length() > 300 ? s.substring(0, 300) + "...(trunc)" : s;
    }
}
