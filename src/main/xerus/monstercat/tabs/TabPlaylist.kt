package xerus.monstercat.tabs

import javafx.beans.Observable
import javafx.beans.value.ObservableValue
import javafx.scene.control.*
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.util.Callback
import xerus.ktutil.javafx.MenuItem
import xerus.ktutil.javafx.fill
import xerus.ktutil.javafx.properties.ImmutableObservable
import xerus.monstercat.api.Player
import xerus.monstercat.api.Playlist
import xerus.monstercat.api.response.Track


class TabPlaylist : VTab() {
	var table = TableView<Track>().apply {
		columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
	}
	
	init {
		table.items = Playlist.tracks
		
		table.columns.addAll(TableColumn<Track, String>("Artists").apply {
			cellValueFactory = Callback<TableColumn.CellDataFeatures<Track, String>, ObservableValue<String>> { p ->
				ImmutableObservable(p.value.artistsTitle)
			}
		}, TableColumn<Track, String>("Title").apply {
			cellValueFactory = Callback<TableColumn.CellDataFeatures<Track, String>, ObservableValue<String>> { p ->
				ImmutableObservable(p.value.title)
			}
		})
		
		table.setRowFactory {
			TableRow<Track>().apply {
				Playlist.currentIndex.addListener { _, _, newValue ->
					style = if (index == newValue) {
						"-fx-background-color: #1f6601"
					} else {
						"-fx-background-color: transparent"
					}
				}
			}
		}
		
		table.selectionModel.selectionMode = SelectionMode.SINGLE
		
		table.setOnMouseClicked { me ->
			if (me.button == MouseButton.PRIMARY && me.clickCount == 2) {
				Player.playFromPlaylist(table.selectionModel.selectedIndex)
			}
			if (me.button == MouseButton.MIDDLE && me.clickCount == 1) {
				Playlist.removeAt(table.selectionModel.selectedIndex)
			}
		}
		
		table.setOnKeyPressed { ke ->
			if (ke.code == KeyCode.DELETE){
				Playlist.removeAt(table.selectionModel.selectedIndex)
			}else if (ke.code == KeyCode.ENTER){
				Player.playFromPlaylist(table.selectionModel.selectedIndex)
			}else if (ke.code == KeyCode.ADD || ke.code == KeyCode.PLUS){
				useSelectedTrack { Playlist.addNext(it) }
			}
		}
		
		table.placeholder = Label("Your playlist is empty.")
		
		val rightClickMenu = ContextMenu()
		val item1 = MenuItem("Play") {
			Player.playFromPlaylist(table.selectionModel.selectedIndex)
		}
		val item2 = MenuItem("Play Next") {
			useSelectedTrack { Playlist.addNext(it) }
		}
		val item3 = MenuItem("Remove") {
			Playlist.removeAt(table.selectionModel.selectedIndex)
		}
		val item4 = MenuItem("Clear playlist") {
			Playlist.clear()
			Player.reset()
		}
		rightClickMenu.items.addAll(item1, item2, item3, item4)
		table.contextMenu = rightClickMenu
		
		fill(table)
	}
	
	inline fun useSelectedTrack(action: (Track) -> Unit) {
		action(table.selectionModel.selectedItem)
	}
	
}