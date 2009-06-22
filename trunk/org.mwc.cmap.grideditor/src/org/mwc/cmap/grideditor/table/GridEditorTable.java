package org.mwc.cmap.grideditor.table;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.mwc.cmap.grideditor.GridEditorActionContext;
import org.mwc.cmap.grideditor.table.actons.GridEditorActionGroup;
import org.mwc.cmap.grideditor.table.actons.TablePopupMenuBuilder;
import org.mwc.cmap.gridharness.data.GriddableSeries;


public class GridEditorTable extends Composite {

	private final TableViewer myTableViewer;

	private final TableModel myTableModel;

	private final SeriesLabelProvider mySeriesLabelProvider;

	private final GridEditorActionContext myActionContext;

	private SelectionListener myColumnHeaderSelectionListener;

	private boolean myIsTrackingSelection = true;

	public GridEditorTable(Composite parent, GridEditorActionGroup actionGroup) {
		super(parent, SWT.NONE);
		setTrackingSelection(true);
		setLayout(new FillLayout());
		myActionContext = actionGroup.getContext();

		myTableViewer = new TableViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		myTableViewer.getTable().setHeaderVisible(true);
		myTableViewer.getTable().setLinesVisible(true);
		myTableViewer.setContentProvider(new SeriesContentProvider());
		myTableModel = new TableModel(myTableViewer, myActionContext.getUndoSupport());
		mySeriesLabelProvider = new SeriesLabelProvider(myTableModel);
		myTableViewer.setLabelProvider(mySeriesLabelProvider);
		myTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				myActionContext.setSelection(myTableViewer.getSelection());
			}
		});

		new TablePopupMenuBuilder(this, actionGroup);

		myTableViewer.getTable().addListener(SWT.MeasureItem, new Listener() {

			private int myComputedHeight = -1;

			@Override
			public void handleEvent(Event event) {
				if (myComputedHeight < 0) {
					DateTime shouldFit = new DateTime(myTableViewer.getTable(), SWT.DATE | SWT.MEDIUM);
					Point sizeToFit = shouldFit.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
					shouldFit.dispose();
					myComputedHeight = sizeToFit.y;
				}
				event.height = myComputedHeight;
			}
		});
	}

	public void setTrackingSelection(boolean isTrackingSelection) {
		myIsTrackingSelection = isTrackingSelection;
	}

	public boolean isTrackingSelection() {
		return myIsTrackingSelection;
	}

	public void setColumnHeaderSelectionListener(SelectionListener listener) {
		myColumnHeaderSelectionListener = listener;
	}

	public void setInput(GriddableSeries series) {
		myTableModel.setInputSeries(series);
		//the set of columns was probably changed 
		refreshTableColumns();
		myTableViewer.setInput(series);
		myActionContext.setInput(series);
	}

	public TableViewer getTableViewer() {
		return myTableViewer;
	}

	public TableModel getTableModel() {
		return myTableModel;
	}

	private void refreshTableColumns() {
		//this sets up the cell label providers for all columns at once 
		myTableViewer.setLabelProvider(mySeriesLabelProvider);

		if (myColumnHeaderSelectionListener != null) {
			for (TableColumn next : myTableViewer.getTable().getColumns()) {
				next.addSelectionListener(myColumnHeaderSelectionListener);
			}
		}
	}
}
