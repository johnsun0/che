/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.kubernetes.namespace.pvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.api.workspace.shared.event.WorkspaceRemovedEvent;
import org.eclipse.che.workspace.infrastructure.kubernetes.namespace.KubernetesNamespaceFactory;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/**
 * Tests {@link WorkspacePVCCleaner}.
 *
 * @author Anton Korneta
 */
@Listeners(MockitoTestNGListener.class)
public class WorkspacePVCCleanerTest {

  @Mock private WorkspaceVolumesStrategy pvcStrategy;
  @Mock private EventService eventService;
  @Mock private KubernetesNamespaceFactory namespaceFactory;
  @Mock private Workspace workspace;

  private WorkspacePVCCleaner workspacePVCCleaner;

  @BeforeMethod
  public void setUp() {
    when(namespaceFactory.isPredefined()).thenReturn(true);
    workspacePVCCleaner = new WorkspacePVCCleaner(true, namespaceFactory, pvcStrategy);
  }

  @Test
  public void testDoNotSubscribesCleanerWhenPVCDisabled() {
    workspacePVCCleaner = spy(new WorkspacePVCCleaner(false, namespaceFactory, pvcStrategy));

    workspacePVCCleaner.subscribe(eventService);

    verify(eventService, never()).subscribe(any(), eq(WorkspaceRemovedEvent.class));
  }

  @Test
  public void testDoNotSubscribesCleanerWhenPVCEnabledAndNamespaceIsNotPredefined() {
    when(namespaceFactory.isPredefined()).thenReturn(false);
    workspacePVCCleaner = spy(new WorkspacePVCCleaner(false, namespaceFactory, pvcStrategy));

    workspacePVCCleaner.subscribe(eventService);

    verify(eventService, never()).subscribe(any(), eq(WorkspaceRemovedEvent.class));
  }

  @Test
  public void testSubscribesDeleteKubernetesOnWorkspaceRemove() throws Exception {
    workspacePVCCleaner.subscribe(eventService);

    verify(eventService).subscribe(any(), eq(WorkspaceRemovedEvent.class));
  }

  @Test
  public void testInvokeCleanupWhenWorkspaceRemovedEventPublished() throws Exception {
    doAnswer(
            invocationOnMock -> {
              final EventSubscriber<WorkspaceRemovedEvent> argument =
                  invocationOnMock.getArgument(0);
              final WorkspaceRemovedEvent event = mock(WorkspaceRemovedEvent.class);
              when(event.getWorkspace()).thenReturn(workspace);
              argument.onEvent(event);
              return invocationOnMock;
            })
        .when(eventService)
        .subscribe(any(), eq(WorkspaceRemovedEvent.class));

    workspacePVCCleaner.subscribe(eventService);

    verify(pvcStrategy).cleanup(workspace);
  }

  @Test
  public void testDoNotRethrowExceptionWhenErrorOnCleanupOccurs() throws Exception {
    doAnswer(
            invocationOnMock -> {
              final EventSubscriber<WorkspaceRemovedEvent> argument =
                  invocationOnMock.getArgument(0);
              final WorkspaceRemovedEvent event = mock(WorkspaceRemovedEvent.class);
              when(event.getWorkspace()).thenReturn(workspace);
              argument.onEvent(event);
              return invocationOnMock;
            })
        .when(eventService)
        .subscribe(any(), eq(WorkspaceRemovedEvent.class));
    doThrow(InfrastructureException.class).when(pvcStrategy).cleanup(workspace);

    workspacePVCCleaner.subscribe(eventService);

    verify(pvcStrategy).cleanup(workspace);
  }
}
