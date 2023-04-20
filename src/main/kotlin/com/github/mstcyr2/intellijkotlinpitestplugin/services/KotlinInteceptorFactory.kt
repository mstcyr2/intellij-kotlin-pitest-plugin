package com.github.mstcyr2.intellijkotlinpitestplugin.services

import org.pitest.mutationtest.build.InterceptorParameters
import org.pitest.mutationtest.build.MutationInterceptor
import org.pitest.mutationtest.build.MutationInterceptorFactory
import org.pitest.plugin.Feature


class KotlinInterceptorFactory : MutationInterceptorFactory {
    override fun createInterceptor(interceptorParameters: InterceptorParameters): MutationInterceptor {
        return KotlinInterceptor()
    }

    override fun provides(): Feature {
        return Feature.named("KOTLIN")
                .withOnByDefault(true)
                .withDescription("Improves support of kotlin language")
    }

    override fun description(): String {
        return "Kotlin language support"
    }
}