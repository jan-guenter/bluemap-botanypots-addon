scoreboard players set #checked botany_gallery 0
scoreboard players set #failures botany_gallery 0
function botanypots_gallery:verify_00
function botanypots_gallery:verify_01
function botanypots_gallery:verify_02
function botanypots_gallery:verify_03
execute unless score #shells botany_gallery matches 183 run scoreboard players add #failures botany_gallery 1
execute unless score #representatives botany_gallery matches 6 run scoreboard players add #failures botany_gallery 1
execute unless score #fallbacks botany_gallery matches 3 run scoreboard players add #failures botany_gallery 1
execute unless score #checked botany_gallery matches 192 run scoreboard players add #failures botany_gallery 1
