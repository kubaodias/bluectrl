#!/usr/bin/env python
#-*- coding: iso-8859-2 -*-

# BlueCtrl_server.py
# Kuba Odias
# 2008
# based on pybluez rfcomm-server.py example (http://code.google.com/p/pybluez/)

from bluetooth import *
import time
import commands
import os
import random

class BlueCtrl:
	"""Class used to manage BlueCtrl server"""
	
	playlist_file_name = "playlist.xml"
	BLUETOOTH_PACKET_SIZE = 64

	def __init__(self):
		"""Constructor of the BlueCtrl class objects."""
		self.check_if_player_is_running()	# if music player is not running then this script is terminated
		self.MAX_TRIES = 3
		self.sleep_time = 500

	def check_if_player_is_running(self):
		"""Check if player is open and responds"""
		app_response = commands.getstatusoutput('dcop amarok')[1] 	# check if music player is running
		if app_response.startswith("No such application"):
			print "amarok not running"
			sys.exit()

	def generate_uuid(self):
		"""Generate unique UUID"""
		uuid_parts = []
		sep = '-'
		hex_signs = range(48, 58)	# 10 digit signs - 48 is ASCII for '0'
		hex_signs.extend(range(65,71))	# 6 letters - 'A' to 'F'
		
		hex_asciis = []		# convert hex_signs array to ASCII
		for i in hex_signs:
			hex_asciis.append(chr(i))

		for count in [ 8, 4 ]:		# first 8 and 4 bytes are random
			uuid_part = random.sample(hex_asciis, count)	# generate each part of an UUID
			uuid_parts.append(''.join(uuid_part))
			
		# generate 4-byte part of an UUID with the first first two bits set to 10
		uuid_part = [ chr(random.randint(65,70)) ]
		uuid_part.extend(random.sample(hex_asciis, 3))
		uuid_parts.append(''.join(uuid_part))	
		uuid = sep.join(uuid_parts)

		# generate 4-byte part of an UUID with the first character set as '8'
		uuid_part = ['8']
		uuid_part.extend(random.sample(hex_asciis, 3))
		uuid_parts.append(''.join(uuid_part))	
		uuid = sep.join(uuid_parts)

		# generate the last, 12-byte part of an UUID - it should be a local Bluetooth Device Address, but can't get it properly with PyBluez
		uuid_part = random.sample(hex_asciis, 12)
		uuid_parts.append(''.join(uuid_part))	

		uuid = sep.join(uuid_parts)	# join all parts with '-' character

		return uuid


	def start_server(self):
		"""Start the BlueCtrl server."""

		self.server_sock = BluetoothSocket( RFCOMM )
		self.server_sock.bind(("",PORT_ANY))
		self.server_sock.listen(1)

		port = self.server_sock.getsockname()[1]

		# Set UUID and check if it is correct
		self.uuid = self.generate_uuid()

		if is_valid_uuid(self.uuid) == False:
			print '\"%s\" is not a correct UUID!' % self.uuid
			sys.exit()

		advertise_service( self.server_sock, "BlueCtrl Server",
				   service_id = self.uuid,
				   service_classes = [ self.uuid, SERIAL_PORT_CLASS ],
				   profiles = [ SERIAL_PORT_PROFILE ] 
				    )
			
		print "RFCOMM Service UUID: \"%s\"" % self.uuid	   
		print "Waiting for connection on RFCOMM channel %d" % port

		self.client_sock, self.client_info = self.server_sock.accept()

		self.connected = True
	

	def close_connection(self):
		"""Close the connection with Bluetooth client device."""

		self.client_sock.close()
		self.server_sock.close()
	
		if self.connected == True:
			print "Connection to ", self.client_info, " closed"
		self.connected = False


	def check_connection_with_client(self):
		"""Wait for the connection string from client."""

		try:
			length = self.client_sock.recv(1)
			data = self.client_sock.recv(ord(length))
			if data == "CONNECT":
				self.bluetooth_send_acknowledge("ACK")
				print "Accepted connection from ", self.client_info
				self.bluetooth_send_command("CONNECT_ACK")
			else:
				print "Expecting connection string, got something else - ", data
				self.bluetooth_send_acknowledge("ERROR")
				self.close_connection()
				sys.exit()
		except IOError:
			print "IOError: check_connection_with_client"
			self.close_connection()
	

	def wait_for_command(self):
		"""Wait for a command sent by client."""

		try:
			self.client_sock.setblocking(False)
			length = self.client_sock.recv(1)
			self.client_sock.setblocking(True)
		except IOError:
			return
	
		try:
			command = self.client_sock.recv(ord(length))
			print "Received %s" % command

			if command == "ACK" or command == "ERROR":
				self.bluetooth_send_acknowledge("ERROR")
				return

			self.bluetooth_send_acknowledge("ACK")

			if command == "COMMAND_PLAY":
				commands.getstatusoutput('dcop amarok player play')
			elif command == "COMMAND_PAUSE":
				commands.getstatusoutput('dcop amarok player pause')
			elif command == "COMMAND_NEXT":
				commands.getstatusoutput('dcop amarok player next')
			elif command == "COMMAND_PREV":	
				commands.getstatusoutput('dcop amarok player prev')
			elif command == "COMMAND_VOLUME_UP":	
				commands.getstatusoutput('dcop amarok player volumeUp')
			elif command == "COMMAND_VOLUME_DOWN":	
				commands.getstatusoutput('dcop amarok player volumeDown')
			elif command == "COMMAND_GET_MEDIA_LIBRARY":
				self.parse_playlist()
				self.bluetooth_send_command_with_data("COMMAND_GET_MEDIA_LIBRARY", "playlist")
		except IOError:
			print "IOError: wait_for_command"
			self.bluetooth_send_acknowledge("ERROR")
			self.close_connection()


	def bluetooth_send_acknowledge(self, data):
		"""Send acknowledge to client."""

		try:
			print "Sending acknowledge: ", data
			self.client_sock.send(chr(len(data)))
			length=self.client_sock.send(data)
			print "Sent ", length, " bytes"
			if self.sleep_time > 0.2:
				self.sleep_time = 0.2
			elif self.sleep_time > 0.05:
				self.sleep_time = self.sleep_time - 0.05
		except IOError:
			print "IOError: bluetooth_send_acknowledge(%s)" % data
			self.close_connection()


	def bluetooth_send_command(self, data):
		"""Send command to client."""

		if data != '':	
			try:
				print "Sending command ", data
				self.client_sock.send(chr(len(data)))
				length = self.client_sock.send(data)

				if self.sleep_time > 0.2:
					self.sleep_time = 0.2
				elif self.sleep_time > 0.05:
					self.sleep_time = self.sleep_time - 0.05

				if self.check_acknowledge() == "ERROR":
					return

				print "Sent ", length, " bytes"
			except IOError:
				print "IOError: bluetooth_send_command(%s)" % data
				self.close_connection()


	def bluetooth_send_command_with_data(self, command, data):
		"""Send command followed by data to client."""

		playlist_to_be_sent = False

		if data == "title":	
			data = self.title
		elif data == "artist":
			data = self.artist
		elif data == "total time":
			data = commands.getstatusoutput('dcop amarok player totalTime')[1]
			if data == "":
				return
		elif data == "current time":
			data = commands.getstatusoutput('dcop amarok player currentTime')[1]
			if data == "":
				return
		elif data == "volume level":
			data = self.volume_level
		# send size of the playlist file
		elif data == "playlist":	
			data = str(os.stat(self.playlist_file_name)[6])
			playlist_to_be_sent = True

		if data == "":
			data = ' '

		try:
			print "Sending command ", command
			self.client_sock.send(chr(len(command)))	

			length = self.client_sock.send(command)
		
			if self.sleep_time > 0.2:
				self.sleep_time = 0.2
			elif self.sleep_time > 0.05:
				self.sleep_time = self.sleep_time - 0.05

			if self.check_acknowledge() == "ERROR":
				return 

			print "Sent ", length, " bytes"

			print "Sending data: ", data
			self.client_sock.send(chr(len(data)))
			length = self.client_sock.send(data)

			if self.sleep_time > 0.2:
				self.sleep_time = 0.2
			elif self.sleep_time > 0.05:
				self.sleep_time = self.sleep_time - 0.05

			if self.check_acknowledge() == "ERROR":
				return

			print "Sent ", length, " bytes"

			if playlist_to_be_sent == True:
				self.send_playlist()
		except IOError:
			print "IOError: bluetooth_send_command_with_data(%s)" % command + ", " + data
			self.close_connection()


	def check_acknowledge(self):
		"""Check for acknowledge few times."""

		for i in range(0, self.MAX_TRIES):
			ret = self.wait_for_acknowledge()
			if (ret == "ERROR") or (ret == "TIMEOUT"):
				print "Acknowledge number %d failed" % i
			elif ret == "RETRANSMIT":
				break
			elif ret == "ACK":
				print "Acknowledge received"
				break

		if (ret == "ERROR") or (ret == "TIMEOUT"):
			print "Acknowledge error!!"
			self.close_connection()
			return "ERROR"
		elif ret == "RETRANSMIT":
			return "ERROR"
		else:
			return "OK"


	def wait_for_acknowledge(self):
		"""Wait for acknowledge sent by client."""

		timeout = self.client_sock.gettimeout()
		try:
			self.client_sock.settimeout(5)
			length = self.client_sock.recv(1)
			self.client_sock.settimeout(timeout)
		except IOError:
			return "TIMEOUT"

		try:
			command = self.client_sock.recv(ord(length))
			print "Received acknowledge: %s" % command

			if self.sleep_time > 0.2:
				self.sleep_time = 0.2
			elif self.sleep_time > 0.05:
				self.sleep_time = self.sleep_time - 0.05

			if command == "ACK":
				return "ACK"
			elif command == "ERROR":
				return "RETRANSMIT"
			else:
				self.bluetooth_send_acknowledge("ERROR")
				return "ERROR"
		except IOError:
			print "IOError: wait_for_acknowledge"
			self.close_connection()
			return "ERROR"
	

	def send_player_data(self):
		"""Send data about music being played to client."""
	
		self.title = commands.getstatusoutput('dcop amarok player title')[1]
		self.artist = commands.getstatusoutput('dcop amarok player artist')[1]
		print "Playing ", self.title, " by ", self.artist
		self.bluetooth_send_command_with_data("COMMAND_GET_TITLE", "title")
		self.bluetooth_send_command_with_data("COMMAND_GET_ARTIST", "artist")
		self.bluetooth_send_command_with_data("COMMAND_GET_TOTAL_TIME", "total time")


	def check_player_status(self):
		"""Check if user changed player status on PC."""

		tmp_status = commands.getstatusoutput('dcop amarok player isPlaying')[1]
		tmp_title = commands.getstatusoutput('dcop amarok player title')[1]
		tmp_artist = commands.getstatusoutput('dcop amarok player artist')[1]
		tmp_volume_level = commands.getstatusoutput('dcop amarok player getVolume')[1]

		# if player changed it's status on a PC
		if ((tmp_status == "false") and (self.is_playing == True)) or ((tmp_status == "true") and 
				(self.is_playing == False)) or (tmp_title != self.title) or (tmp_artist != self.artist):
	
			if tmp_status == "false":	# set beginning state of the player
				self.is_playing = False
			elif tmp_status == "true":
				self.is_playing = True

			if self.is_playing == True:
				if (tmp_title != self.title) or (tmp_artist != self.artist):
					self.bluetooth_send_command_with_data("COMMAND_CHANGE_STATE", "OTHER")
				else:	
					self.bluetooth_send_command_with_data("COMMAND_CHANGE_STATE", "PLAY")
			elif self.is_playing == False:	# player is paused or stopped
				if commands.getstatusoutput('dcop amarok player totalTime')[1] == "?":
					self.bluetooth_send_command_with_data("COMMAND_CHANGE_STATE", "STOP")
				else:
					self.bluetooth_send_command_with_data("COMMAND_CHANGE_STATE", "PAUSE")
				

			self.send_player_data()		# send data about the music track

			blue_ctrl.bluetooth_send_command_with_data("COMMAND_GET_CURRENT_TIME", "current time")


		if tmp_volume_level != self.volume_level:
			self.volume_level = tmp_volume_level
			self.bluetooth_send_command_with_data("COMMAND_GET_VOLUME_LEVEL", "volume level")			
		


	def parse_playlist(self):
		"""Save and parse a playlist."""

		try:		
			filename = commands.getstatusoutput('dcop amarok playlist saveCurrentPlaylist')[1]
			try:
				playlist_input_file = open(filename, "r")
			except IOError:
				print "IOError: parse_playlist - open file"
				return

			playlist_output_file = open(self.playlist_file_name, "w")

			i = 0
			for line in playlist_input_file:	# check each line in input file and write the most important lines to playlist.xml
				if line.lstrip().startswith("<?xml") == True:		# beginning of a XML file
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("<playlist") == True:	# beginning of the playlist
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("</playlist>") == True:	# end of the playlist
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("<item") == True:		# beginning of the music track
					#line = "<item uniqueid=" + line.split("uniqueid=")[1]
					playlist_output_file.write("<item>\n<id>" + str(i) + "</id>\n")
				elif line.lstrip().startswith("</item>") == True:	# end of the music track
					playlist_output_file.write(line.lstrip())
					i = i + 1	# track added to playlist
				elif line.lstrip().startswith("<Length>") == True:	# length of the track
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("<Title>") == True:	# title of the song
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("<Artist>") == True:	# artist name
					playlist_output_file.write(line.lstrip())
				elif line.lstrip().startswith("<Album>") == True:	# album name
					playlist_output_file.write(line.lstrip())
		except IOError:
			print "IOError: parse_playlist"
			
		# close opened files
		playlist_input_file.close()	
		playlist_output_file.close()	


	def send_playlist(self):
		"""Send playlist over Bluetooth"""

		try:
			playlist_input_file = open(self.playlist_file_name, "r")
		except IOError:
			print "IOError: send_playlist - open file"
			return
		
		size = os.stat(self.playlist_file_name)[6]
		i = 0
		bytes_added = 0
		str_to_send = []

		print "Sending of playlist in progress..."
		while i < size:
			c = playlist_input_file.read(1)

			# send '?' instead of unicode characters
			if ord(c) > 127:
				c = '?'
		
			str_to_send.append(c)
			bytes_added = bytes_added + 1

			if (bytes_added == self.BLUETOOTH_PACKET_SIZE) or (i == size - 1):
				self.client_sock.send(''.join(str_to_send))
				str_to_send = []
				bytes_added = 0

			i = i + 1

		print "Sent %d bytes of playlist file" % size
		# close playlist file
		playlist_input_file.close()



##################################### MAIN PROGRAM ######################################

if __name__ == '__main__':
	blue_ctrl = BlueCtrl()	# create BlueCtrl object

	i = 0
	while True:
		# set beginning state of the player
		blue_ctrl.connected = False
		blue_ctrl.is_playing = False
		blue_ctrl.title = ""
		blue_ctrl.artist = ""
		blue_ctrl.volume_level = commands.getstatusoutput('dcop amarok player getVolume')[1]

		blue_ctrl.start_server()
		blue_ctrl.check_connection_with_client()

		while blue_ctrl.connected == True:
			if blue_ctrl.sleep_time < 0.2:
				blue_ctrl.sleep_time = blue_ctrl.sleep_time + 0.05
			elif blue_ctrl.sleep_time < 0.5:
				blue_ctrl.sleep_time = blue_ctrl.sleep_time + 0.1

			blue_ctrl.wait_for_command()
			blue_ctrl.check_player_status()
			time.sleep(blue_ctrl.sleep_time)
			i = i + 1
			if i > 15:
				i = 0
				blue_ctrl.bluetooth_send_command_with_data("COMMAND_GET_CURRENT_TIME", "current time")
		
		print "Connection broken. Trying again in 3 seconds..."
		time.sleep(3)
