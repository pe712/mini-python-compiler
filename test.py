# Définition de la fonction avec des paramètres obligatoires et optionnels
def ma_fonction(parametre_obligatoire, parametre_optionnel1="valeur_par_defaut1", parametre_optionnel2="valeur_par_defaut2"):
    print("Paramètre obligatoire:" + parametre_obligatoire)
    print("Paramètre optionnel 1:" + parametre_optionnel1)
    print("Paramètre optionnel 2:" + parametre_optionnel2)

# Tests avec différentes combinaisons de paramètres
# Test 1 : Fournir uniquement le paramètre obligatoire
print("Test 1")
ma_fonction("obligatoire")

# Test 2 : Fournir tous les paramètres
print("Test 2")
ma_fonction("obligatoire", "optionnel1", "optionnel2")

# Test 3 : Fournir le paramètre obligatoire et un paramètre optionnel
print("Test 3")
ma_fonction("obligatoire", "optionnel1")

# Test 4 : Fournir le paramètre obligatoire et un autre paramètre optionnel
print("Test 4")
ma_fonction("obligatoire", parametre_optionnel2="optionnel2")

# Test 5 : Fournir le paramètre obligatoire et spécifier les paramètres optionnels dans un ordre différent
print("Test 5")
ma_fonction("obligatoire", parametre_optionnel2="optionnel2", parametre_optionnel1="optionnel1")

# # Test 6 : Appel de la fonction avec un nombre incorrect de paramètres
# # Cela doit générer une erreur TypeError
print("Test 6")
ma_fonction("obligatoire", paramètre_optionnel2 = "optionnel2", "optionnel1")

# # Test 7 : Appel de la fonction avec un nombre incorrect de paramètres
# # Cela doit générer une erreur TypeError
# try:
#     ma_fonction()
# except TypeError as e:
#     print("Erreur:", e)

# try:
#     ma_fonction("obligatoire", "optionnel1", "optionnel2", "trop_de_parametres")
# except TypeError as e:
#     print("Erreur:", e)
