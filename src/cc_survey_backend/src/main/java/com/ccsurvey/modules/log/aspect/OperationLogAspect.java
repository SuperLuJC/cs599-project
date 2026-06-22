package com.ccsurvey.modules.log.aspect;

import com.alibaba.fastjson2.JSON;
import com.ccsurvey.common.response.TraceContext;
import com.ccsurvey.common.util.IpUtils;
import com.ccsurvey.modules.log.annotation.OperLog;
import com.ccsurvey.modules.log.entity.OperationLog;
import com.ccsurvey.modules.log.service.AsyncLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AsyncLogService asyncLogService;

    /**
     * 处理完请求后执行
     */
    @AfterReturning(pointcut = "@annotation(operLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, OperLog operLog, Object jsonResult) {
        handleLog(joinPoint, operLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     */
    @AfterThrowing(value = "@annotation(operLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperLog operLog, Exception e) {
        handleLog(joinPoint, operLog, e, null);
    }

    /**
     * 处理日志
     */
    protected void handleLog(JoinPoint joinPoint, OperLog operLog, Exception e, Object jsonResult) {
        try {
            // 获取当前请求
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            // 构建日志对象
            OperationLog log = new OperationLog();
            log.setTraceId(TraceContext.getTraceId());
            log.setTitle(operLog.title());
            log.setBusinessType(operLog.businessType());
            log.setRequestMethod(request.getMethod());
            log.setOperUrl(request.getRequestURI());
            log.setOperIp(IpUtils.getClientIp(request));
            log.setOperTime(LocalDateTime.now());

            // 获取用户信息
            String userId = (String) request.getAttribute("userId");
            String name = (String) request.getAttribute("name");

            // 如果用户未登录，标记为匿名用户
            if (userId == null) {
                log.setOperUserId("anonymous");
                log.setOperName("匿名用户");
            } else {
                log.setOperUserId(userId);
                log.setOperName(name != null ? name : "用户");
            }

            // 方法名称
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            log.setMethod(className + "." + methodName + "()");

            // 请求参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                Map<String, Object> params = new HashMap<>();
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof HttpServletRequest ||
                        arg instanceof HttpServletResponse ||
                        arg instanceof MultipartFile) {
                        continue;
                    }
                    params.put("arg" + i, arg);
                }
                if (!params.isEmpty()) {
                    String paramStr = JSON.toJSONString(params);
                    if (paramStr.length() > 2000) {
                        paramStr = paramStr.substring(0, 2000) + "...";
                    }
                    log.setOperParam(paramStr);
                }
            }

            // 返回结果
            if (jsonResult != null) {
                String resultStr = JSON.toJSONString(jsonResult);
                if (resultStr.length() > 2000) {
                    resultStr = resultStr.substring(0, 2000) + "...";
                }
                log.setJsonResult(resultStr);
            }

            // 异常信息
            if (e != null) {
                log.setStatus(1);
                String errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.length() > 2000) {
                    errorMsg = errorMsg.substring(0, 2000);
                }
                log.setErrorMsg(errorMsg);
            } else {
                log.setStatus(0);
            }

            // 计算耗时
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime != null) {
                log.setCostTime(System.currentTimeMillis() - startTime);
            }

            // 异步保存日志
            asyncLogService.saveLog(log);

        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        }
    }
}