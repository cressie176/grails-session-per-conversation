/*
    This file is part of the grails session-per-conversation plugin.

    session-per-conversation is free software: you can redistribute it and/or modify
    it under the terms of the Lesser GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    session-per-conversation is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    Lesser GNU General Public License for more details.

    You should have received a copy of the Lesser GNU General Public License
    along with AppStatus.  If not, see <http://www.gnu.org/licenses/>.
*/

package uk.co.acuminous.spc

import org.hibernate.event.EventSource
import org.hibernate.classic.Session
import org.hibernate.jdbc.JDBCContext.Context
import org.hibernate.impl.SessionImpl
import org.hibernate.HibernateException
import org.hibernate.Transaction
import org.hibernate.ConnectionReleaseMode
import org.hibernate.engine.ActionQueue
import org.hibernate.persister.entity.EntityPersister
import org.hibernate.engine.EntityEntry
import org.hibernate.type.Type
import org.hibernate.Query
import org.hibernate.EntityMode
import org.hibernate.SessionFactory
import java.sql.Connection
import org.hibernate.LockMode
import org.hibernate.ReplicationMode
import org.hibernate.Criteria
import org.hibernate.SQLQuery
import org.hibernate.Filter
import org.hibernate.stat.SessionStatistics
import org.hibernate.jdbc.Work
import org.hibernate.Interceptor
import org.hibernate.collection.PersistentCollection
import org.hibernate.engine.SessionFactoryImplementor
import org.hibernate.jdbc.Batcher
import org.hibernate.engine.QueryParameters
import org.hibernate.ScrollableResults
import org.hibernate.impl.CriteriaImpl
import org.hibernate.ScrollMode
import org.hibernate.engine.EntityKey
import org.hibernate.loader.custom.CustomQuery
import org.hibernate.engine.query.sql.NativeSQLQuerySpecification
import org.hibernate.event.EventListeners
import org.hibernate.engine.PersistenceContext
import org.hibernate.CacheMode
import org.hibernate.FlushMode
import org.hibernate.jdbc.JDBCContext
import org.apache.log4j.Logger

class ConversationalSession implements EventSource, Session, Context {

    // Delegation was broken in groovy 1.7.4 - http://jira.codehaus.org/browse/GROOVY-4320
    // @Delegate EventSource eventSource
    // @Delegate Session session
    // @Delegate Context context

    static Logger logger = Logger.getLogger(ConversationalSession)
    SessionImpl sessionImpl

    public ConversationalSession(SessionImpl sessionImpl) {
        this.sessionImpl = sessionImpl
    }

    /*
     * Grails makes entities readonly when they fail validation so that invalid data isn't flushed on session close.
     * The entity remains readonly until validation is successful or is reloaded from a different session in a
     * subsequent http request.
     *
     * Making an entity read only has the side effect of quietly suppressing all subsequent saves made with validation off,
     * for it's lifetime within the same session, which is something your application may need to do if it supports
     * persistence draft versions of the entity.
     *
     * This is sensible in a request-per-request model, where the readonly flag will be reset between requests,
     * but not for session-per-conversation, where the flag will last for the duration of the conversation
     */
    public void setReadOnly(Object entity, boolean value) {
        logger.warn('Suppressing call to setReadOnly')        
    }

    /*
     * Grails AbstractSavePersistentMethod flushes when saving entities with validation off, even when flush mode is
     * set to MANUAL / NEVER. This breaks the session-per-conversation. If you need to flush change the flush mode
     * to FlushMode.AUTO first, then change it back again.
     */
    public void flush() throws HibernateException {
        if (flushMode in [FlushMode.MANUAL, FlushMode.NEVER]) {
            logger.warn('Suppressing call to flush')
        } else {
            sessionImpl.flush()
        }
    }

    void afterTransactionBegin(Transaction transaction) {
        sessionImpl.afterTransactionBegin(transaction)
    }

    ConnectionReleaseMode getConnectionReleaseMode() {
        sessionImpl.getConnectionReleaseMode()
    }

    boolean isAutoCloseSessionEnabled() {
        sessionImpl.isAutoCloseSessionEnabled()
    }

    boolean isFlushModeNever() {
        sessionImpl.isFlushModeNever()
    }

    boolean isFlushBeforeCompletionEnabled() {
        sessionImpl.isFlushBeforeCompletionEnabled()
    }

    void managedFlush() {
        sessionImpl.managedFlush()
    }

    boolean shouldAutoClose() {
        sessionImpl.shouldAutoClose()
    }

    void managedClose() {
        sessionImpl.managedClose()
    }

    ActionQueue getActionQueue() {
        sessionImpl.getActionQueue()
    }

    Object instantiate(EntityPersister entityPersister, Serializable serializable) {
        sessionImpl.instantiate(entityPersister, serializable)
    }

    void forceFlush(EntityEntry entityEntry) {
        sessionImpl.forceFlush(entityEntry)
    }

    void merge(String s, Object o, Map map) {
        sessionImpl.merge(s, o, map)
    }

    void persist(String s, Object o, Map map) {
        sessionImpl.persist(s, o, map)
    }

    void persistOnFlush(String s, Object o, Map map) {
        sessionImpl.persistOnFlush(s, o, map)
    }

    void refresh(Object o, Map map) {
        sessionImpl.refresh(o, map)
    }

    void saveOrUpdateCopy(String s, Object o, Map map) {
        sessionImpl.saveOrUpdateCopy(s, o, map)
    }

    void delete(String s, Object o, boolean b, Set set) {
        sessionImpl.delete(s, o, b, set)
    }

    Object saveOrUpdateCopy(Object o) {
        sessionImpl.saveOrUpdateCopy(o)
    }

    Object saveOrUpdateCopy(Object o, Serializable serializable) {
        sessionImpl.saveOrUpdateCopy(o, serializable)
    }

    Object saveOrUpdateCopy(String s, Object o) {
        sessionImpl.saveOrUpdateCopy(s, o)
    }

    Object saveOrUpdateCopy(String s, Object o, Serializable serializable) {
        sessionImpl.saveOrUpdateCopy(s, o, serializable)
    }

    List find(String s) {
        sessionImpl.find(s)
    }

    List find(String s, Object o, Type type) {
        sessionImpl.find(s, o, type)
    }

    List find(String s, Object[] objects, Type[] types) {
        sessionImpl.find(s, objects, types)
    }

    Iterator iterate(String s) {
        sessionImpl.iterate(s)
    }

    Iterator iterate(String s, Object o, Type type) {
        sessionImpl.iterate(s, o, type)
    }

    Iterator iterate(String s, Object[] objects, Type[] types) {
        sessionImpl.iterate(s, objects, types)
    }

    Collection filter(Object o, String s) {
        sessionImpl.filter(o, s)
    }

    Collection filter(Object o, String s, Object o1, Type type) {
        sessionImpl.filter(o, s, o1, type)
    }

    Collection filter(Object o, String s, Object[] objects, Type[] types) {
        sessionImpl.filter(o, s, objects, types)
    }

    int delete(String s) {
        sessionImpl.delete(s)
    }

    int delete(String s, Object o, Type type) {
        sessionImpl.delete(s, o, type)
    }

    int delete(String s, Object[] objects, Type[] types) {
        sessionImpl.delete(s, objects, types)
    }

    Query createSQLQuery(String s, String s1, Class aClass) {
        sessionImpl.createSQLQuery(s, s1, aClass)
    }

    Query createSQLQuery(String s, String[] strings, Class[] classes) {
        sessionImpl.createSQLQuery(s, strings, classes)
    }

    void save(Object o, Serializable serializable) {
        sessionImpl.save(o, serializable)
    }

    void save(String s, Object o, Serializable serializable) {
        sessionImpl.save(s, o, serializable)
    }

    void update(Object o, Serializable serializable) {
        sessionImpl.save(o, serializable)
    }

    void update(String s, Object o, Serializable serializable) {
        sessionImpl.update(s, o, serializable)
    }

    org.hibernate.Session getSession(EntityMode entityMode) {
        sessionImpl.getSession(entityMode)
    }

    SessionFactory getSessionFactory() {
        sessionImpl.getSessionFactory()
    }

    Connection close() {
        sessionImpl.close()
    }

    void cancelQuery() {
        sessionImpl.cancelQuery()
    }

    boolean isDirty() {
        sessionImpl.isDirty()
    }

    Serializable getIdentifier(Object o) {
        sessionImpl.getIdentifier(o)
    }

    boolean contains(Object o) {
        sessionImpl.contains(o)
    }

    void evict(Object o) {
        sessionImpl.evict(o)
    }

    Object load(Class aClass, Serializable serializable, LockMode lockMode) {
        sessionImpl.load(aClass, serializable, lockMode)
    }

    Object load(String s, Serializable serializable, LockMode lockMode) {
        sessionImpl.load(s, serializable, lockMode)
    }

    Object load(Class aClass, Serializable serializable) {
        sessionImpl.load(aClass, serializable)
    }

    Object load(String s, Serializable serializable) {
        sessionImpl.load(s, serializable)
    }

    void load(Object o, Serializable serializable) {
        sessionImpl.load(o, serializable)
    }

    void replicate(Object o, ReplicationMode replicationMode) {
        sessionImpl.replicate(o, replicationMode)
    }

    void replicate(String s, Object o, ReplicationMode replicationMode) {
        sessionImpl.replicate(s, o, replicationMode)
    }

    Serializable save(Object o) {
        sessionImpl.save(o)
    }

    Serializable save(String s, Object o) {
        sessionImpl.save(s, o)
    }

    void saveOrUpdate(Object o) {
        sessionImpl.saveOrUpdate(o)
    }

    void saveOrUpdate(String s, Object o) {
        sessionImpl.saveOrUpdate(s, o)
    }

    void update(Object o) {
        sessionImpl.update(o)
    }

    void update(String s, Object o) {
        sessionImpl.update(s, o)
    }

    Object merge(Object o) {
        sessionImpl.merge(o)
    }

    Object merge(String s, Object o) {
        sessionImpl.merge(s, o)
    }

    void persist(Object o) {
        sessionImpl.persist(o)
    }

    void persist(String s, Object o) {
        sessionImpl.persist(s, o)
    }

    void delete(Object o) {
        sessionImpl.delete(o)
    }

    void delete(String s, Object o) {
        sessionImpl.delete(s, o)
    }

    void lock(Object o, LockMode lockMode) {
        sessionImpl.lock(o, lockMode)
    }

    void lock(String s, Object o, LockMode lockMode) {
        sessionImpl.lock(s, o, lockMode)
    }

    void refresh(Object o) {
        sessionImpl.refresh(o)
    }

    void refresh(Object o, LockMode lockMode) {
        sessionImpl.refresh(o, lockMode)
    }

    LockMode getCurrentLockMode(Object o) {
        sessionImpl.getCurrentLockMode(o)
    }

    Transaction beginTransaction() {
        sessionImpl.beginTransaction()
    }

    Transaction getTransaction() {
        sessionImpl.getTransaction()
    }

    Criteria createCriteria(Class aClass) {
        sessionImpl.createCriteria(aClass)
    }

    Criteria createCriteria(Class aClass, String s) {
        sessionImpl.createCriteria(aClass, s)
    }

    Criteria createCriteria(String s) {
        sessionImpl.createCriteria(s)
    }

    Criteria createCriteria(String s, String s1) {
        sessionImpl.createCriteria(s, s1)
    }

    Query createQuery(String s) {
        sessionImpl.createQuery(s)
    }

    SQLQuery createSQLQuery(String s) {
        sessionImpl.createSQLQuery(s)
    }

    Query createFilter(Object o, String s) {
        sessionImpl.createFilter(o, s)
    }

    void clear() {
        sessionImpl.clear()
    }

    Object get(Class aClass, Serializable serializable) {
        sessionImpl.get(aClass, serializable)
    }

    Object get(Class aClass, Serializable serializable, LockMode lockMode) {
        sessionImpl.get(aClass, serializable, lockMode)
    }

    Object get(String s, Serializable serializable) {
        sessionImpl.get(s, serializable)
    }

    Object get(String s, Serializable serializable, LockMode lockMode) {
        sessionImpl.get(s, serializable, lockMode)
    }

    String getEntityName(Object o) {
        sessionImpl.getEntityName(o)
    }

    Filter enableFilter(String s) {
        sessionImpl.enableFilter(s)
    }

    Filter getEnabledFilter(String s) {
        sessionImpl.getEnabledFilter(s)
    }

    void disableFilter(String s) {
        sessionImpl.disableFilter(s)
    }

    SessionStatistics getStatistics() {
        sessionImpl.getStatistics()
    }

    void doWork(Work work) {
        sessionImpl.doWork(work)
    }

    Connection disconnect() {
        sessionImpl.disconnect()
    }

    void reconnect() {
        sessionImpl.reconnect()
    }

    void reconnect(Connection connection) {
        sessionImpl.reconnect(connection)
    }

    Interceptor getInterceptor() {
        sessionImpl.getInterceptor()
    }

    void setAutoClear(boolean b) {
        sessionImpl.setAutoClear(b)
    }

    boolean isTransactionInProgress() {
        sessionImpl.isTransactionInProgress()
    }

    void initializeCollection(PersistentCollection persistentCollection, boolean b) {
        sessionImpl.initializeCollection(persistentCollection, b)
    }

    Object internalLoad(String s, Serializable serializable, boolean b, boolean b1) {
        sessionImpl.internalLoad(s, serializable, b, b1)
    }

    Object immediateLoad(String s, Serializable serializable) {
        sessionImpl.immediateLoad(s, serializable)
    }

    long getTimestamp() {
        sessionImpl.getTimestamp()
    }

    SessionFactoryImplementor getFactory() {
        sessionImpl.getFactory()
    }

    Batcher getBatcher() {
        sessionImpl.getBatcher()
    }

    List list(String s, QueryParameters queryParameters) {
        sessionImpl.list(s, queryParameters)
    }

    Iterator iterate(String s, QueryParameters queryParameters) {
        sessionImpl.iterate(s, queryParameters)
    }

    ScrollableResults scroll(String s, QueryParameters queryParameters) {
        sessionImpl.scroll(s, queryParameters)
    }

    ScrollableResults scroll(CriteriaImpl criteria, ScrollMode scrollMode) {
        sessionImpl.scroll(criteria, scrollMode)
    }

    List list(CriteriaImpl criteria) {
        sessionImpl.list(criteria)
    }

    List listFilter(Object o, String s, QueryParameters queryParameters) {
        sessionImpl.listFilter(o, s, queryParameters)
    }

    Iterator iterateFilter(Object o, String s, QueryParameters queryParameters) {
        sessionImpl.iterateFilter(o, s, queryParameters)
    }

    EntityPersister getEntityPersister(String s, Object o) {
        sessionImpl.getEntityPersister(s, o)
    }

    Object getEntityUsingInterceptor(EntityKey entityKey) {
        sessionImpl.getEntityUsingInterceptor(entityKey)
    }

    void afterTransactionCompletion(boolean b, Transaction transaction) {
        sessionImpl.afterTransactionCompletion(b, transaction)
    }

    void beforeTransactionCompletion(Transaction transaction) {
        sessionImpl.beforeTransactionCompletion(transaction)
    }

    Serializable getContextEntityIdentifier(Object o) {
        sessionImpl.getContextEntityIdentifier(o)
    }

    String bestGuessEntityName(Object o) {
        sessionImpl.bestGuessEntityName(o)
    }

    String guessEntityName(Object o) {
        sessionImpl.guessEntityName(o)
    }

    Object instantiate(String s, Serializable serializable) {
        sessionImpl.instantiate(s, serializable)
    }

    List listCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) {
        sessionImpl.listCustomQuery(customQuery, queryParameters)
    }

    ScrollableResults scrollCustomQuery(CustomQuery customQuery, QueryParameters queryParameters) {
        sessionImpl.scrollCustomQuery(customQuery, queryParameters)
    }

    List list(NativeSQLQuerySpecification nativeSQLQuerySpecification, QueryParameters queryParameters) {
        sessionImpl.list(nativeSQLQuerySpecification, queryParameters)
    }

    ScrollableResults scroll(NativeSQLQuerySpecification nativeSQLQuerySpecification, QueryParameters queryParameters) {
        sessionImpl.scroll(nativeSQLQuerySpecification, queryParameters)
    }

    Object getFilterParameterValue(String s) {
        sessionImpl.getFilterParameterValue(s)
    }

    Type getFilterParameterType(String s) {
        sessionImpl.getFilterParameterType(s)
    }

    Map getEnabledFilters() {
        sessionImpl.getEnabledFilters()
    }

    int getDontFlushFromFind() {
        sessionImpl.getDontFlushFromFind()
    }

    EventListeners getListeners() {
        sessionImpl.getListeners()
    }

    PersistenceContext getPersistenceContext() {
        sessionImpl.getPersistenceContext()
    }

    int executeUpdate(String s, QueryParameters queryParameters) {
        sessionImpl.executeUpdate(s, queryParameters)
    }

    int executeNativeUpdate(NativeSQLQuerySpecification nativeSQLQuerySpecification, QueryParameters queryParameters) {
        sessionImpl.executeNativeUpdate(nativeSQLQuerySpecification, queryParameters)
    }

    EntityMode getEntityMode() {
        sessionImpl.getEntityMode()
    }

    CacheMode getCacheMode() {
        sessionImpl.getCacheMode()
    }

    void setCacheMode(CacheMode cacheMode) {
        sessionImpl.setCacheMode(cacheMode)
    }

    boolean isOpen() {
        sessionImpl.isOpen()
    }

    boolean isConnected() {
        sessionImpl.isConnected()
    }

    FlushMode getFlushMode() {
        sessionImpl.getFlushMode()
    }

    void setFlushMode(FlushMode flushMode) {
        sessionImpl.setFlushMode(flushMode)
    }

    Connection connection() {
        sessionImpl.connection()
    }

    Query getNamedQuery(String s) {
        sessionImpl.getNamedQuery(s)
    }

    Query getNamedSQLQuery(String s) {
        sessionImpl.getNamedSQLQuery(s)
    }

    boolean isEventSource() {
        sessionImpl.isEventSource()
    }

    void afterScrollOperation() {
        sessionImpl.afterScrollOperation()
    }

    void setFetchProfile(String s) {
        sessionImpl.setFetchProfile(s)
    }

    String getFetchProfile() {
        sessionImpl.getFetchProfile()
    }

    JDBCContext getJDBCContext() {
        sessionImpl.getJDBCContext()
    }

    boolean isClosed() {
        sessionImpl.isClosed()
    }
}
