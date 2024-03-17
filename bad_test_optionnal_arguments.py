# Définition de la fonction avec des paramètres obligatoires et optionnels
def ma_fonction(parametre_obligatoire, parametre_optionnel1="valeur_par_defaut1", parametre_optionnel2="valeur_par_defaut2"):
    print("Paramètre obligatoire:" + parametre_obligatoire)
    print("Paramètre optionnel 1:" + parametre_optionnel1)
    print("Paramètre optionnel 2:" + parametre_optionnel2)

# Tests avec différentes combinaisons de paramètres qui doivent toutes échouer
# Test 1 : Fournir trop de paramètres
ma_fonction("obligatoire", "autre1", "autre2", "trop" )

# # Test 2 : Positional argument cannot appear after keyword arguments
# ma_fonction("obligatoire", parametre_optionnel1="optionnel1", "optionnel2")

# # Test 3 : Manque un argument obligatoire
# ma_fonction()
