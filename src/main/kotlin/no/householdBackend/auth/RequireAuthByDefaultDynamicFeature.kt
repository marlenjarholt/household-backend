package no.householdBackend.auth

import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.container.DynamicFeature
import javax.ws.rs.container.ResourceInfo
import javax.ws.rs.core.FeatureContext

class RequireAuthByDefaultDynamicFeature(private val authFilter: ContainerRequestFilter) : DynamicFeature {
    override fun configure(resourceInfo: ResourceInfo, context: FeatureContext) {
        val annotationOnClass = resourceInfo.resourceClass.getAnnotation(NoAuthRequired::class.java) != null
        val annotationOnMethod = resourceInfo.resourceMethod.getAnnotation(NoAuthRequired::class.java) != null

        if (!annotationOnClass && !annotationOnMethod) {
            context.register(authFilter)
        }
    }
}

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION
)

annotation class NoAuthRequired
