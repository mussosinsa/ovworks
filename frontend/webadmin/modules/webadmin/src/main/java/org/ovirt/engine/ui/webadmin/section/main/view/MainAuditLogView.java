package org.ovirt.engine.ui.webadmin.section.main.view;

import org.ovirt.engine.ui.common.uicommon.model.MainModelProvider;
import org.ovirt.engine.ui.common.widget.table.SimpleActionTable;
import org.ovirt.engine.ui.common.widget.table.column.AbstractTextColumn;
import org.ovirt.engine.ui.uicommonweb.models.AuditLogListModel;
import org.ovirt.engine.ui.webadmin.gin.ClientGinjectorProvider;
import org.ovirt.engine.ui.webadmin.section.main.presenter.MainAuditLogPresenter;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogProtectionTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AuditLogRemoteBackupTabView;
import org.ovirt.engine.ui.webadmin.section.main.view.popup.security.AvailabilityTabView;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.inject.Inject;

public class MainAuditLogView extends AbstractMainWithDetailsTableView<Object, AuditLogListModel>
        implements MainAuditLogPresenter.ViewDef {

    private final AuditLogProtectionTabView auditLogProtectionTabView;
    private final AuditLogRemoteBackupTabView auditLogRemoteBackupTabView;
    private final AvailabilityTabView availabilityTabView;

    @Override
    protected SimpleActionTable<Void, Object> createActionTable() {
        SimpleActionTable<Void, Object> table = new SimpleActionTable<Void, Object>(getModelProvider(),
                getTableResources(), ClientGinjectorProvider.getEventBus(),
                ClientGinjectorProvider.getClientStorage()) {
            {
                showRefreshButton();
                showItemsCount();
                showSelectionCountTooltip();
                enableHeaderContextMenu();
            }
        };

        // 데이터가 있어도 컬럼이 없을 때 렌더링 에러를 방지하기 위한 더미 컬럼
        table.addColumn(new AbstractTextColumn<Object>() {
            @Override
            public String getValue(Object object) {
                return ""; //$NON-NLS-1$
            }
        }, ""); //$NON-NLS-1$

        return table;
    }

    @Inject
    public MainAuditLogView(MainModelProvider<Object, AuditLogListModel> modelProvider,
            AuditLogProtectionTabView auditLogProtectionTabView,
            AuditLogRemoteBackupTabView auditLogRemoteBackupTabView,
            AvailabilityTabView availabilityTabView) {
        super(modelProvider);

        this.auditLogProtectionTabView = auditLogProtectionTabView;
        this.auditLogRemoteBackupTabView = auditLogRemoteBackupTabView;
        this.availabilityTabView = availabilityTabView;

        // 기본 테이블 숨김
        getTable().setVisible(false);

        // 메인 컨테이너 생성 (전체 너비, 수직 스크롤 가능)
        FlowPanel mainContainer = new FlowPanel();
        mainContainer.getElement().getStyle().setProperty("width", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("height", "100%"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("flexDirection", "column"); //$NON-NLS-1$ //$NON-NLS-2$
        mainContainer.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$

        // 1. 상단 헤더 (이미지와 동일하게 유지)
        FlowPanel header = new FlowPanel();
        header.getElement().getStyle().setProperty("display", "flex"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("justifyContent", "space-between"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("alignItems", "center"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("padding", "10px 15px"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("borderBottom", "1px solid #ddd"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("backgroundColor", "#f8f8f8"); //$NON-NLS-1$ //$NON-NLS-2$
        header.getElement().getStyle().setProperty("flexShrink", "0"); //$NON-NLS-1$ //$NON-NLS-2$

        HTML headerTitle = new HTML("관리 - 감사기록 관리"); //$NON-NLS-1$
        headerTitle.getElement().getStyle().setProperty("fontWeight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
        headerTitle.getElement().getStyle().setProperty("fontSize", "16px"); //$NON-NLS-1$ //$NON-NLS-2$
        header.add(headerTitle);

        HTML headerClose = new HTML("&times;"); //$NON-NLS-1$
        headerClose.getElement().getStyle().setProperty("color", "#777"); //$NON-NLS-1$ //$NON-NLS-2$
        headerClose.getElement().getStyle().setProperty("fontSize", "18px"); //$NON-NLS-1$ //$NON-NLS-2$
        headerClose.getElement().getStyle().setProperty("cursor", "default"); //$NON-NLS-1$ //$NON-NLS-2$
        header.add(headerClose);

        mainContainer.add(header);

        // 2. 바디 영역 (사이드바 제거, 수직 나열 구조)
        FlowPanel contentBody = new FlowPanel();
        contentBody.getElement().getStyle().setProperty("padding", "20px"); //$NON-NLS-1$ //$NON-NLS-2$
        contentBody.getElement().getStyle().setProperty("overflowY", "auto"); //$NON-NLS-1$ //$NON-NLS-2$
        contentBody.getElement().getStyle().setProperty("flex", "1"); //$NON-NLS-1$ //$NON-NLS-2$

        // 각 섹션을 생성하여 바디에 추가 (이미지 순서대로: 보호 -> 원격 백업 -> 가용성)
        contentBody.add(createSectionPanel("감사기록 보호", auditLogProtectionTabView)); //$NON-NLS-1$
        contentBody.add(createSectionPanel("감사기록 원격 백업", auditLogRemoteBackupTabView)); //$NON-NLS-1$
        contentBody.add(createSectionPanel("가용성 확보", availabilityTabView)); //$NON-NLS-1$

        mainContainer.add(contentBody);

        // 메인 뷰에 추가
        getTable().getOuterWidget().add(mainContainer);
        mainContainer.setVisible(true);

        initWidget(getTable());
    }

    /**
     * 이미지의 각 섹션(제목 + 박스 형태)을 생성하는 헬퍼 메서드
     */
    private FlowPanel createSectionPanel(String title, IsWidget contentWidget) {
        FlowPanel sectionPanel = new FlowPanel();

        // 섹션 간 간격 및 스타일
        sectionPanel.getElement().getStyle().setProperty("marginBottom", "25px"); //$NON-NLS-1$ //$NON-NLS-2$

        // 1. 섹션 제목
        HTML titleHtml = new HTML(title);
        titleHtml.getElement().getStyle().setProperty("fontSize", "18px"); //$NON-NLS-1$ //$NON-NLS-2$
        titleHtml.getElement().getStyle().setProperty("fontWeight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
        titleHtml.getElement().getStyle().setProperty("marginBottom", "10px"); //$NON-NLS-1$ //$NON-NLS-2$
        titleHtml.getElement().getStyle().setProperty("color", "#333"); //$NON-NLS-1$ //$NON-NLS-2$
        sectionPanel.add(titleHtml);

        // 2. 콘텐츠 컨테이너 (이미지의 흰색 박스 및 테두리 느낌)
        FlowPanel contentContainer = new FlowPanel();
        contentContainer.getElement().getStyle().setProperty("border", "1px solid #e0e0e0"); //$NON-NLS-1$ //$NON-NLS-2$
        contentContainer.getElement().getStyle().setProperty("padding", "15px"); //$NON-NLS-1$ //$NON-NLS-2$
        contentContainer.getElement().getStyle().setProperty("backgroundColor", "#ffffff"); //$NON-NLS-1$ //$NON-NLS-2$
        contentContainer.getElement().getStyle().setProperty("borderRadius", "2px"); //$NON-NLS-1$ //$NON-NLS-2$

        // 주입받은 실제 뷰(버튼 등이 포함된 뷰) 추가
        contentContainer.add(contentWidget);

        sectionPanel.add(contentContainer);

        return sectionPanel;
    }
}
