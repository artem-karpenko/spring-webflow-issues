package org.springframework.webflow.issues.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.persistence.JpaFlowExecutionListener;

public class FixedJpaFlowExecutionListener extends JpaFlowExecutionListener {
//    public FixedJpaFlowExecutionListener(EntityManagerFactory entityManagerFactory,
//            PlatformTransactionManager transactionManager) {
//        super(entityManagerFactory, transactionManager);
//    }
//    
//    @Override
//    public void paused(RequestContext context) {
//        super.paused(context);
//        EntityManager entityManager = (EntityManager) context.getFlowScope().get(PERSISTENCE_CONTEXT_ATTRIBUTE);
//        if (entityManager != null && entityManager instanceof HibernateEntityManager) {
//            HibernateEntityManager hibernateEntityManager = (HibernateEntityManager) entityManager;
//            hibernateEntityManager.getSession().disconnect();
//        }
//    }
    
    private TransactionTemplate transactionTemplate;

    public FixedJpaFlowExecutionListener(EntityManagerFactory entityManagerFactory,
            PlatformTransactionManager transactionManager) {

        super(entityManagerFactory, transactionManager);

        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void paused(RequestContext context) {

        if (isPersistenceContext(context.getActiveFlow())) {

            final EntityManager em = getEntityManager(context.getFlowExecutionContext().getActiveSession());

            /*
             * we were not be able to determine if entityManager has opened
             * connections, then the commit statement is always executed when
             * flow is paused
             */

            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    /*
                     * Commit using the same connection retrieved by hibernate
                     * lazy load,if exists, otherwise another connection will be
                     * requested from pool
                     */
                    em.joinTransaction();
                }
            });

        }

        super.paused(context);
    }

    private boolean isPersistenceContext(FlowDefinition flow) {
        return flow.getAttributes().contains(PERSISTENCE_CONTEXT_ATTRIBUTE);
    }

    private EntityManager getEntityManager(FlowSession session) {
        return (EntityManager) session.getScope().get(PERSISTENCE_CONTEXT_ATTRIBUTE);
    }
}
