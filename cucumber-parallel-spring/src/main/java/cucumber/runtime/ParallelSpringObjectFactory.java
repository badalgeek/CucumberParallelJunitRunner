package cucumber.runtime;

import cucumber.api.java.ObjectFactory;
import io.badal.cucumber.CucumberConfigProvider;
import io.badal.cucumber.CucumberProperties;
import io.badal.cucumber.spring.SpringParentContextProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Collection;
import java.util.HashSet;

/**
 * Created by badal on 12/18/15.
 */
public class ParallelSpringObjectFactory implements ObjectFactory {

    private ClassPathXmlApplicationContext applicationContext;
    private final Collection<Class<?>> stepClasses = new HashSet();

    @Override
    public void start() {
        CucumberConfigProvider instance = CucumberConfigProvider.getInstance();
        ApplicationContext parentSpringContext = SpringParentContextProvider.getParentSpringContext(
                instance.getStringArrayProperty(CucumberProperties.SpringParentContext.name()));
        this.applicationContext = new ClassPathXmlApplicationContext(
                instance.getStringArrayProperty(CucumberProperties.SpringChildContext.name()), parentSpringContext);

        for(Class stepClass : this.stepClasses) {
            this.registerStepClassBeanDefinition(stepClass);
        }
    }

    private void registerStepClassBeanDefinition(Class<?> stepClass) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(stepClass)
                .setScope(BeanDefinition.SCOPE_SINGLETON)
                .setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE)
                .setLazyInit(true)
                .getBeanDefinition();
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        if(!(beanFactory instanceof BeanDefinitionRegistry)) {
            throw new RuntimeException("Spring Context do not allow bean registry");
        }
        BeanDefinitionRegistry registry = ((BeanDefinitionRegistry ) beanFactory);
        registry.registerBeanDefinition(stepClass.getName(), beanDefinition);
    }

    @Override
    public void stop() {
        this.applicationContext.close();
    }

    @Override
    public boolean addClass(Class<?> stepClass) {
        if(!this.stepClasses.contains(stepClass)) {
            this.stepClasses.add(stepClass);
        }
        return true;
    }

    @Override
    public <T> T getInstance(Class<T> glueClass) {
        return this.applicationContext.getBean(glueClass.getName(), glueClass);
    }
}
